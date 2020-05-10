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
  token.payload.user == username
  user_owns_token
}

# Allow managers to get their subordinate's salaries.
allow {
  some username
  input.method == "GET"
  input.path = ["finance", "salary", username]
  token.payload.subordinates[_] == username
  user_owns_token
}

# Allow HR members to get anyone's salary.
allow {
  input.method == "GET"
  input.path = ["finance", "salary", _]
  token.payload.hr == true
  user_owns_token
}

# Ensure that the token was issued to the user supplying it.
user_owns_token { input.user == token.payload.azp }

# Helper to get the token payload.
token = {"payload": payload} {
  [header, payload, signature] := io.jwt.decode(input.token)
}
