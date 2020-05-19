package rules

# bob is alice's manager, and betty is charlie's.
subordinates = {"alice": [], "charlie": [], "bob": ["alice"], "betty": ["charlie"]}

# david is the only member of HR.
hr = [ "david" ]

################
# Main
main["allowed"] = allow

################
# Allow Rules
default allow = false

# allow users to get their own salaries.
allow {
  some username
  input.method == "GET"
  input.path = ["finance", "salary", username]
  input.user == username
}

# allow managers to get their subordinate's salaries.
allow {
  some username
  input.method == "GET"
  input.path = ["finance", "salary", username]
  subordinates[input.user][_] == username
}

# allow HR members to get anyone's salary.
# allow {
#   input.method == "GET"
#   input.path = ["finance", "salary", _]
#   user_is_hr
# }

# allow HR members to access HR dashboard
# allow {
#   input.method == "GET"
#   input.path = ["hr", "dashboard"]
#   user_is_hr
# }

################
# Filter Rules
filter := {
  "allowed": count(filter_data) > 0,
  "data": filter_data
}

# is HR - return all records and all attributes
filter_data[record] {
  user_is_hr
  record := input.data[_]
}

# is not HR - return self
filter_data[record] {
  not user_is_hr
  some i
  input.data[i].name == input.user
  record := input.data[i]
}

# is not HR - return subordinates without ssn
filter_data[record] {
  not user_is_hr
  some i,j
  subordinates[input.user][i] == input.data[j].name
  record := json.remove(input.data[j], ["ssn"])
}

user_is_hr {
  input.user == hr[_]
}
