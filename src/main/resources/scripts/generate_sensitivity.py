# generate_sensitivity.py
import pandas as pd
import sys

print("Python: Generating dummy Sensitivity_Results.xlsx...")
print(f"Python: Current working directory: {sys.path[0]}") # For debugging where it's run from

data = {'Attr_id': ['1', '2', '3', '18', '198'], 'Sensitivity_Level': ['High', 'Medium', 'Low', 'High', 'Medium']}
df = pd.DataFrame(data)

output_filename = "Sensitivity_Results.xlsx"
try:
    df.to_excel(output_filename, index=False, engine='openpyxl')
    print(f"Python: Dummy {output_filename} created successfully.")
except Exception as e:
    print(f"Python: Error creating {output_filename}: {e}")
    sys.exit(1)
