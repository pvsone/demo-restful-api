package rules

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
  jwt.payload.user == username
  user_owns_jwt
}

# Allow managers to get their subordinate's salaries.
allow {
  some username
  input.method == "GET"
  input.path = ["finance", "salary", username]
  jwt.payload.subordinates[_] == username
  user_owns_jwt
}

# Allow HR members to get anyone's salary.
allow {
  input.method == "GET"
  input.path = ["finance", "salary", _]
  jwt.payload.hr == true
  user_owns_jwt
}

# Ensure that the jwt was issued to the user supplying it.
user_owns_jwt { input.user == jwt.payload.azp }

# Helper to get the jwt payload.
jwt = {"payload": payload} {
  [header, payload, signature] := io.jwt.decode(input.jwt)
}
