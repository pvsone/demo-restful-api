package rules

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
  jwt.payload.user == username
}

# allow managers to get their subordinate's salaries.
allow {
  some username
  input.method == "GET"
  input.path = ["finance", "salary", username]
  jwt.payload.subordinates[_] == username
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
  input.data[i].name == jwt.payload.user
  record := input.data[i]
}

# is not HR - return subordinates without ssn
filter_data[record] {
  not user_is_hr
  some i,j
  jwt.payload.subordinates[i] == input.data[j].name
  record := json.remove(input.data[j], ["ssn"])
}

user_is_hr {
  jwt.payload.hr == true
}

# Helper to get the jwt payload.
jwt = {"payload": payload} {
  [header, payload, signature] := io.jwt.decode(input.jwt)
}
