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

# Allow users to get their own salaries.
allow {
  some username
  input.method == "GET"
  input.path = ["finance", "salary", username]
  input.user == username
}

# Allow managers to get their subordinates' salaries.
allow {
  some username
  input.method == "GET"
  input.path = ["finance", "salary", username]
  subordinates[input.user][_] == username
}

# Allow HR members to get anyone's salary.
allow {
  input.method == "GET"
  input.path = ["finance", "salary", _]
  input.user == hr[_]
}

# Allow HR members to access HR dashboard
# allow {
#   input.method == "GET"
#   input.path = ["hr", "dashboard"]
#   input.user == hr[_]
# }
