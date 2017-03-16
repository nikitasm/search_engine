import os, sys


def is_number(s):
	try:
		float(s)
		return True
	except ValueError:
		pass
	try:
		import unicodedata
		unicodedata.numeric(s)
		return True
	except (TypeError, ValueError):
		pass
	return False


print("Starting separator: \n")

files = [f for f in os.listdir('.') if os.path.isfile(f)]

for f in files:
	if f == 'doc-text':
		doc_file = open(f, 'r')
		new_file_exists = False

		for line in doc_file:			
			if is_number(line) and int(line) in range(1,11430):
				new_file_exists = True
				if int(line) > 1:
					file.close()
				print('Creating new text file for paragraph' + line) 
				name = line.replace(' ', '') + ".txt"
				name = line.replace("\n", '') + ".txt"

				try:
					if not os.path.exists("../files/"):
    						os.makedirs("../files/")
					file = open("../files/" + name,'a')
				except Exception:
					print('Something went wrong! Can\'t tell what?')
					sys.exit(0)
					continue
			else:
				if new_file_exists:
					file.write(line)
