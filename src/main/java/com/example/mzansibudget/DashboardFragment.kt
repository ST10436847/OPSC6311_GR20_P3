package com.example.mzansibudget

import android.content.Context
import android.content.SharedPreferences
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.*

class DashboardFragment : Fragment() {

    private lateinit var tvTotalSpent: TextView
    private lateinit var tvStatus: TextView
    private lateinit var etMinGoal: EditText
    private lateinit var etMaxGoal: EditText
    private lateinit var btnSaveGoals: Button
    private lateinit var pbBudget: ProgressBar
    private lateinit var recyclerView: RecyclerView
    private lateinit var btnLogout: Button
    private lateinit var tvPoints: TextView
    private lateinit var tvBadges: TextView
    
    private lateinit var sharedPref: SharedPreferences
    private lateinit var database: AppDatabase
    private var expenseList = mutableListOf<Expense>()
    private var currentUserObj: User? = null

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_dashboard, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        tvTotalSpent = view.findViewById(R.id.tv_total_spent)
        tvStatus = view.findViewById(R.id.tv_status)
        etMinGoal = view.findViewById(R.id.et_min_goal)
        etMaxGoal = view.findViewById(R.id.et_max_goal)
        btnSaveGoals = view.findViewById(R.id.btn_save_goals)
        pbBudget = view.findViewById(R.id.pb_budget)
        recyclerView = view.findViewById(R.id.recycler_view_expenses)
        btnLogout = view.findViewById(R.id.btn_logout)
        tvPoints = view.findViewById(R.id.tv_points)
        tvBadges = view.findViewById(R.id.tv_badges)

        sharedPref = requireActivity().getSharedPreferences("MzansiBudgetPrefs", Context.MODE_PRIVATE)
        database = AppDatabase.getDatabase(requireContext())

        recyclerView.layoutManager = LinearLayoutManager(requireContext())

        btnSaveGoals.setOnClickListener {
            saveGoals()
        }

        btnLogout.setOnClickListener {
            sharedPref.edit().clear().apply()
            (activity as MainActivity).showBottomNav(false)
            (activity as MainActivity).loadFragment(LoginFragment())
        }

        loadUserData()
    }

    private fun loadUserData() {
        val username = sharedPref.getString("currentUser", "") ?: ""
        viewLifecycleOwner.lifecycleScope.launch(Dispatchers.IO) {
            currentUserObj = database.userDao().getUserByUsername(username)
            withContext(Dispatchers.Main) {
                currentUserObj?.let {
                    etMinGoal.setText(it.minMonthlyGoal.toString())
                    etMaxGoal.setText(it.maxMonthlyGoal.toString())
                    tvPoints.text = "${it.points} Points"
                    tvBadges.text = if (it.badges.isEmpty()) "No badges yet" else it.badges.replace(";", ", ")
                    loadDashboardData()
                }
            }
        }
    }

    private fun saveGoals() {
        val min = etMinGoal.text.toString().toDoubleOrNull() ?: 0.0
        val max = etMaxGoal.text.toString().toDoubleOrNull() ?: 0.0
        
        currentUserObj?.let { user ->
            val updatedUser = user.copy(minMonthlyGoal = min, maxMonthlyGoal = max)
            viewLifecycleOwner.lifecycleScope.launch(Dispatchers.IO) {
                database.userDao().updateUser(updatedUser)
                currentUserObj = updatedUser
                withContext(Dispatchers.Main) {
                    Toast.makeText(requireContext(), "Goals saved", Toast.LENGTH_SHORT).show()
                    loadDashboardData()
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        loadUserData()
    }

    private fun loadDashboardData() {
        val currentUser = sharedPref.getString("currentUser", "") ?: ""

        viewLifecycleOwner.lifecycleScope.launch(Dispatchers.IO) {
            val cal = Calendar.getInstance()
            val endStr = String.format("%d-%02d-%02d", cal.get(Calendar.YEAR), cal.get(Calendar.MONTH) + 1, cal.get(Calendar.DAY_OF_MONTH))
            cal.add(Calendar.DAY_OF_YEAR, -30)
            val startStr = String.format("%d-%02d-%02d", cal.get(Calendar.YEAR), cal.get(Calendar.MONTH) + 1, cal.get(Calendar.DAY_OF_MONTH))
            
            val expensesInPeriod = database.expenseDao().getExpensesByPeriod(currentUser, startStr, endStr)
            val totalSpentInMonth = expensesInPeriod.sumOf { it.amount }
            
            val allExpenses = database.expenseDao().getExpensesByUser(currentUser)

            withContext(Dispatchers.Main) {
                if (!isAdded) return@withContext
                
                tvTotalSpent.text = "Total Spent (Past Month): R${"%.2f".format(totalSpentInMonth)}"
                
                currentUserObj?.let { user ->
                    when {
                        totalSpentInMonth < user.minMonthlyGoal -> {
                            tvStatus.text = "Status: Below Minimum Goal"
                            tvStatus.setTextColor(ContextCompat.getColor(requireContext(), android.R.color.holo_blue_dark))
                        }
                        totalSpentInMonth <= user.maxMonthlyGoal -> {
                            tvStatus.text = "Status: Within Target Range"
                            tvStatus.setTextColor(ContextCompat.getColor(requireContext(), android.R.color.holo_green_dark))
                        }
                        else -> {
                            tvStatus.text = "Status: Above Maximum Goal"
                            tvStatus.setTextColor(ContextCompat.getColor(requireContext(), android.R.color.holo_red_dark))
                        }
                    }
                    
                    if (user.maxMonthlyGoal > 0) {
                        pbBudget.max = user.maxMonthlyGoal.toInt()
                        pbBudget.progress = totalSpentInMonth.toInt()
                    } else {
                        pbBudget.progress = 0
                    }
                }

                expenseList.clear()
                expenseList.addAll(allExpenses.take(10)) 
                recyclerView.adapter = ExpenseAdapter(
                    expenseList = expenseList,
                    onItemClick = { expense ->
                        if (expense.receiptImage != null) {
                            showImageDialog(expense.receiptImage)
                        } else {
                            Toast.makeText(requireContext(), "${expense.description}: R${"%.2f".format(expense.amount)}", Toast.LENGTH_SHORT).show()
                        }
                    },
                    onItemLongClick = { expense ->
                        showDeleteConfirmation(expense)
                    }
                )
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
                        loadDashboardData()
                    }
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
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