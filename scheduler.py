import tkinter as tk
from tkinter import ttk, messagebox
import random

# -----------------------------
# Constants
# -----------------------------
DAYS = ["Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday", "Sunday"]
SHIFTS = ["Morning", "Afternoon", "Evening"]
MIN_EMP_PER_SHIFT = 2          # Minimum employees required per shift
MAX_DAYS_PER_EMPLOYEE = 5      # Maximum working days per employee per week

# -----------------------------
# Main Scheduler Application
# -----------------------------
class SchedulerApp:
    def __init__(self, root):
        # Initialize main window
        self.root = root
        self.root.title("Employee Shift Scheduler")
        
        # Data structures
        self.employees = {}  # {name: {day: [ranked shifts]}}
        self.schedule = {day: {shift: [] for shift in SHIFTS} for day in DAYS}
        
        # Create GUI input frame
        self.create_input_frame()
    
    # -----------------------------
    # Create GUI Input Frame
    # -----------------------------
    def create_input_frame(self):
        self.input_frame = ttk.Frame(self.root, padding=10)
        self.input_frame.grid(row=0, column=0, sticky="NSEW")
        
        # Employee name entry
        ttk.Label(self.input_frame, text="Employee Name:").grid(row=0, column=0)
        self.name_entry = ttk.Entry(self.input_frame)
        self.name_entry.grid(row=0, column=1, columnspan=3)
        
        # Shift preference selection for each day
        self.pref_vars = {}  # Holds StringVars for each day's ranked preferences
        row_offset = 1
        for i, day in enumerate(DAYS):
            ttk.Label(self.input_frame, text=day).grid(row=row_offset+i, column=0, sticky="W")
            var1 = tk.StringVar(value=SHIFTS[0])
            var2 = tk.StringVar(value=SHIFTS[1])
            var3 = tk.StringVar(value=SHIFTS[2])
            self.pref_vars[day] = [var1, var2, var3]
            ttk.OptionMenu(self.input_frame, var1, SHIFTS[0], *SHIFTS).grid(row=row_offset+i, column=1)
            ttk.OptionMenu(self.input_frame, var2, SHIFTS[1], *SHIFTS).grid(row=row_offset+i, column=2)
            ttk.OptionMenu(self.input_frame, var3, SHIFTS[2], *SHIFTS).grid(row=row_offset+i, column=3)
        
        # Buttons for adding employees, generating schedule, and resetting
        ttk.Button(self.input_frame, text="Add Employee", command=self.add_employee).grid(row=row_offset+len(DAYS), column=0, pady=10)
        ttk.Button(self.input_frame, text="Generate Schedule", command=self.generate_schedule).grid(row=row_offset+len(DAYS), column=1, columnspan=2)
        ttk.Button(self.input_frame, text="Reset Scheduler", command=self.reset_scheduler).grid(row=row_offset+len(DAYS), column=3, pady=10)
    
    # -----------------------------
    # Add Employee
    # -----------------------------
    def add_employee(self):
        name = self.name_entry.get().strip()
        if not name:
            messagebox.showwarning("Input Error", "Please enter employee name.")
            return
        if name in self.employees:
            messagebox.showwarning("Input Error", "Employee already added.")
            return
        
        # Store ranked shift preferences for each day
        self.employees[name] = {}
        for day in DAYS:
            ranked_shifts = [var.get().strip().capitalize() for var in self.pref_vars[day]]
            if len(set(ranked_shifts)) < 3:  # Ensure unique rankings
                messagebox.showwarning("Input Error", f"Please choose unique shift ranks for {day}.")
                return
            self.employees[name][day] = ranked_shifts
        
        messagebox.showinfo("Employee Added", f"{name} added successfully.")
        self.name_entry.delete(0, tk.END)
    
    # -----------------------------
    # Reset Scheduler
    # -----------------------------
    def reset_scheduler(self):
        """Reset all employee data and schedule"""
        self.employees.clear()
        self.schedule = {day: {shift: [] for shift in SHIFTS} for day in DAYS}
        self.name_entry.delete(0, tk.END)
        messagebox.showinfo("Scheduler Reset", "All employee data and schedules have been cleared. You can add new employees now.")
    
    # -----------------------------
    # Generate Schedule
    # -----------------------------
    def generate_schedule(self):
        if not self.employees:
            messagebox.showwarning("No Employees", "Please add at least one employee.")
            return
        
        # Reset schedule and tracking
        self.schedule = {day: {shift: [] for shift in SHIFTS} for day in DAYS}
        days_worked = {name: 0 for name in self.employees}
        assigned_today = {day: set() for day in DAYS}

        # Step 1: Assign employees based on ranked preferences (1st -> 2nd -> 3rd)
        for rank in range(3):  # 0=first, 1=second, 2=third preference
            for day in DAYS:
                for shift in SHIFTS:
                    # Employees eligible for this shift and day
                    candidates = [
                        name for name in self.employees
                        if days_worked[name] < MAX_DAYS_PER_EMPLOYEE
                        and name not in assigned_today[day]
                        and self.employees[name][day][rank] == shift
                    ]
                    random.shuffle(candidates)  # Randomize to balance workload
                    for name in candidates:
                        self.schedule[day][shift].append(name)
                        assigned_today[day].add(name)
                        days_worked[name] += 1
                        if len(self.schedule[day][shift]) >= MIN_EMP_PER_SHIFT:
                            break

        # Step 2: Fill remaining shifts randomly to meet minimum employee requirement
        for day in DAYS:
            for shift in SHIFTS:
                while len(self.schedule[day][shift]) < MIN_EMP_PER_SHIFT:
                    available = [
                        name for name in self.employees
                        if days_worked[name] < MAX_DAYS_PER_EMPLOYEE
                        and name not in assigned_today[day]
                    ]
                    if not available:
                        break
                    chosen = random.choice(available)
                    self.schedule[day][shift].append(chosen)
                    assigned_today[day].add(chosen)
                    days_worked[chosen] += 1

        # Show schedule in a new window
        self.show_schedule()
    
    # -----------------------------
    # Display Schedule
    # -----------------------------
    def show_schedule(self):
        schedule_window = tk.Toplevel(self.root)
        schedule_window.title("Weekly Schedule")
        
        for i, day in enumerate(DAYS):
            ttk.Label(schedule_window, text=day, font=("Arial", 10, "bold")).grid(row=i*4, column=0, sticky="W", pady=5)
            for j, shift in enumerate(SHIFTS):
                employees = ", ".join(self.schedule[day][shift])
                ttk.Label(schedule_window, text=f"{shift}: {employees}").grid(row=i*4+j+1, column=0, sticky="W")

# -----------------------------
# Run the Application
# -----------------------------
if __name__ == "__main__":
    root = tk.Tk()
    app = SchedulerApp(root)
    root.mainloop()
