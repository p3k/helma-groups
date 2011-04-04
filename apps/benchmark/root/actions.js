function main_action () {
	header ();
	res.writeln ( group.size ("sessions") + " elements in the group" );
	res.writeln ( " group.sessions has " + group.sessions.count () + " child(ren)" );
}

function print_action () {
	header ();
	res.write ("<pre>" + group.getContent ("sessions") + "</pre>");
}

// log in a user:
// (userdata is constructed in app.data at startup)
function login_action () {
	header ();
	this.login ();
}

// log in req.data.x users:
function loginx_action () {
	header ();
	var starttime = new Date ();
	for (var i=0;i<req.data.x;i++) {
      if (i%100 == 0) {
         app.log("logging in user " + i);
      }
		this.login();
	}
	res.writeln ("added " + req.data.x + " users");
	res.writeln ("took " + ((new Date()) - starttime) + " millis");
}

function login () {
	var usr =  nextUser ();
	if (group.sessions[usr.cookie]!=null) {
	   group.sessions[usr.cookie].lastActive = new Date ();
	}  else {
   	var obj = new GroupObject ();
	   obj.username = usr.username;
	   obj.email = usr.email;
	   obj.cookie = usr.cookie;
	   obj.lastActive = new Date ();
	   group.sessions[usr.cookie] = obj;
	}
}


// log out a random user:
function logout_action () {
	header ();
	var usr = randomUser ();
	if (group.sessions[usr.cookie]!=null) {
	   group.sessions[usr.cookie] = null;
	   res.writeln ( "logged out " + usr.username );
	}  else {
	   res.writeln ( "nothing to do for " + usr.username);
	}
}




// simulate reading access:
function check_action () {
	header ();
	// get a random user and check him:
	var usr =  randomUser ();
	if (group.sessions[usr.cookie]!=null) {
	   group.sessions[usr.cookie].lastActive = new Date ();
	   res.writeln ( "updated " + usr.username );
	}  else {
		res.writeln ( usr.username + " not logged in");
	}
}


// used by benchmark tool
function nothing_action () {
	res.write ("nothing");
}


function testfunction (arg1, arg2) {
   return "a result from " + app.__app__.getName ()+ " at " + new Date ();
}

