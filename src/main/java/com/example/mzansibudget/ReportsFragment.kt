package com.example.mzansibudget

import android.app.DatePickerDialog
import android.content.Context
import android.content.SharedPreferences
import android.graphics.Color
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.github.mikephil.charting.charts.BarChart
import com.github.mikephil.charting.components.LimitLine
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.BarData
import com.github.mikephil.charting.data.BarDataSet
import com.github.mikephil.charting.data.BarEntry
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter
import com.github.mikephil.charting.utils.ColorTemplate
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.*

class ReportsFragment : Fragment() {

    private lateinit var sharedPref: SharedPreferences
    private lateinit var llCategoryList: LinearLayout
    private lateinit var tvNoData: TextView
    private lateinit var btnStartDate: Button
    private lateinit var btnEndDate: Button
    private lateinit var rvDetailedExpenses: RecyclerView
    private lateinit var barChart: BarChart
    private lateinit var tvGoalComparison: TextView
    private lateinit var etSearch: EditText
    private lateinit var database: AppDatabase

    private var startDate: Calendar = Calendar.getInstance().apply { add(Calendar.MONTH, -1) }
    private var endDate: Calendar = Calendar.getInstance()
    private val dateFormatter = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
    
    private var allDetailedExpenses = listOf<Expense>()
    private lateinit var adapter: ExpenseAdapter

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_reports, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        llCategoryList = view.findViewById(R.id.ll_category_list)
        tvNoData = view.findViewById(R.id.tv_no_data)
        btnStartDate = view.findViewById(R.id.btn_start_date)
        btnEndDate = view.findViewById(R.id.btn_end_date)
        rvDetailedExpenses = view.findViewById(R.id.rv_detailed_expenses)
        barChart = view.findViewById(R.id.bar_chart)
        tvGoalComparison = view.findViewById(R.id.tv_goal_comparison)
        etSearch = view.findViewById(R.id.et_search_expenses)

        sharedPref = requireActivity().getSharedPreferences("MzansiBudgetPrefs", Context.MODE_PRIVATE)
        database = AppDatabase.getDatabase(requireContext())

        rvDetailedExpenses.layoutManager = LinearLayoutManager(requireContext())
        adapter = ExpenseAdapter(emptyList(), 
            { expense ->
                if (expense.receiptImage != null) {
                    showImageDialog(expense.receiptImage)
                } else {
                    val currencyFormat = NumberFormat.getCurrencyInstance(Locale("en", "ZA"))
                    Toast.makeText(requireContext(), "${expense.description}: ${currencyFormat.format(expense.amount)}", Toast.LENGTH_SHORT).show()
                }
            },
            { expense ->
                showDeleteConfirmation(expense)
            }
        )
        rvDetailedExpenses.adapter = adapter

        updateDateButtons()

        btnStartDate.setOnClickListener {
            showDatePicker(startDate) {
                updateDateButtons()
                loadReportData()
            }
        }

        btnEndDate.setOnClickListener {
            showDatePicker(endDate) {
                updateDateButtons()
                loadReportData()
            }
        }

        etSearch.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                filterExpenses(s.toString())
            }
            override fun afterTextChanged(s: Editable?) {}
        })

        loadReportData()
    }

    private fun filterExpenses(query: String) {
        val filteredList = if (query.isEmpty()) {
            allDetailedExpenses
        } else {
            allDetailedExpenses.filter { it.description.contains(query, ignoreCase = true) || it.category.contains(query, ignoreCase = true) }
        }
        adapter.updateData(filteredList)
    }

    private fun updateDateButtons() {
        btnStartDate.text = "From: ${dateFormatter.format(startDate.time)}"
        btnEndDate.text = "To: ${dateFormatter.format(endDate.time)}"
    }

    private fun showDatePicker(calendar: Calendar, onDateSelected: () -> Unit) {
        DatePickerDialog(requireContext(), { _, year, month, dayOfMonth ->
            calendar.set(year, month, dayOfMonth)
            onDateSelected()
        }, calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH), calendar.get(Calendar.DAY_OF_MONTH)).show()
    }

    private fun loadReportData() {
        val currentUser = sharedPref.getString("currentUser", "") ?: ""
        val startStr = dateFormatter.format(startDate.time)
        val endStr = dateFormatter.format(endDate.time)
        
        viewLifecycleOwner.lifecycleScope.launch(Dispatchers.IO) {
            val categoryTotals = database.expenseDao().getCategoryTotalsByPeriod(currentUser, startStr, endStr)
            allDetailedExpenses = database.expenseDao().getExpensesByPeriod(currentUser, startStr, endStr)
            val user = database.userDao().getUserByUsername(currentUser)

            withContext(Dispatchers.Main) {
                if (!isAdded) return@withContext
                
                setupChart(categoryTotals, user)
                
                llCategoryList.removeAllViews()
                val currencyFormat = NumberFormat.getCurrencyInstance(Locale("en", "ZA"))

                if (categoryTotals.isEmpty()) {
                    tvNoData.visibility = View.VISIBLE
                    barChart.visibility = View.GONE
                } else {
                    tvNoData.visibility = View.GONE
                    barChart.visibility = View.VISIBLE
                    categoryTotals.forEach { total ->
                        val itemView = layoutInflater.inflate(R.layout.item_category_summary, llCategoryList, false)
                        val tvCategory = itemView.findViewById<TextView>(R.id.tv_category_name)
                        val tvAmount = itemView.findViewById<TextView>(R.id.tv_category_amount)

                        tvCategory.text = total.category
                        tvAmount.text = currencyFormat.format(total.total)

                        llCategoryList.addView(itemView)
                    }
                }

                filterExpenses(etSearch.text.toString())
            }
        }
    }

    private fun showDeleteConfirmation(expense: Expense) {
        AlertDialog.Builder(requireContext())
            .setTitle("Delete Expense")
            .setMessage("Are you sure you want to delete this expense?")
            .setPositiveButton("Delete") { _, _ ->
                viewLifecycleOwner.lifecycleScope.launch(Dispatchers.IO) {
                    database.expenseDao().deleteExpense(expense)
                    withContext(Dispatchers.Main) {
                        Toast.makeText(requireContext(), "Expense deleted", Toast.LENGTH_SHORT).show()
                        loadReportData()
                    }
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun setupChart(totals: List<CategoryTotal>, user: User?) {
        val entries = mutableListOf<BarEntry>()
        val labels = mutableListOf<String>()

        totals.forEachIndexed { index, total ->
            entries.add(BarEntry(index.toFloat(), total.total.toFloat()))
            labels.add(total.category)
        }

        val dataSet = BarDataSet(entries, "Spent per Category (R)")
        dataSet.colors = ColorTemplate.MATERIAL_COLORS.toList()
        dataSet.valueTextSize = 10f

        val barData = BarData(dataSet)
        barChart.data = barData
        
        barChart.xAxis.apply {
            valueFormatter = IndexAxisValueFormatter(labels)
            position = XAxis.XAxisPosition.BOTTOM
            granularity = 1f
            setDrawGridLines(false)
            labelRotationAngle = -45f
        }

        barChart.axisLeft.apply {
            removeAllLimitLines()
            user?.let {
                if (it.minMonthlyGoal > 0) {
                    val minLine = LimitLine(it.minMonthlyGoal.toFloat(), "Min Goal")
                    minLine.lineColor = Color.BLUE
                    minLine.lineWidth = 2f
                    minLine.textColor = Color.BLUE
                    addLimitLine(minLine)
                }
                if (it.maxMonthlyGoal > 0) {
                    val maxLine = LimitLine(it.maxMonthlyGoal.toFloat(), "Max Goal")
                    maxLine.lineColor = Color.RED
                    maxLine.lineWidth = 2f
                    maxLine.textColor = Color.RED
                    addLimitLine(maxLine)
                }
            }
            axisMinimum = 0f
        }
        
        barChart.axisRight.isEnabled = false
        barChart.description.isEnabled = false
        barChart.animateY(1000)
        barChart.invalidate()
        
        val totalSpent = totals.sumOf { it.total }
        user?.let {
            tvGoalComparison.text = "Total Spent in period: R${"%.2f".format(totalSpent)}\n" +
                    "Min Goal: R${"%.2f".format(it.minMonthlyGoal)} | Max Goal: R${"%.2f".format(it.maxMonthlyGoal)}"
        }
    }

    private fun showImageDialog(imageBytes: ByteArray) {
        val dialog = android.app.Dialog(requireContext())
        val imageView = ImageView(requireContext())
        val bitmap = android.graphics.BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.size)
        imageView.setImageBitmap(bitmap)
        dialog.setContentView(imageView)
        dialog.window?.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
        dialog.show()
    }
}