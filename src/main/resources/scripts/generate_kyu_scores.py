# generate_kyu_scores.py
import pandas as pd
import sys

print("Python: Generating dummy KYU Score.xlsx...")
print(f"Python: Current working directory: {sys.path[0]}") # For debugging

data = {'ID': ['1', '2', '3'], 'KYU_Score': ['low', 'moderate', 'high']}
df = pd.DataFrame(data)

output_filename = "KYU Score.xlsx" # Ensure this matches the constant in Main.java
try:
    df.to_excel(output_filename, index=False, engine='openpyxl')
    print(f"Python: Dummy {output_filename} created successfully.")
except Exception as e:
    print(f"Python: Error creating {output_filename}: {e}")
    sys.exit(1)
