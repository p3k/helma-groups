
function scheduler () {
}

function onStart () {
   app.data.poolsize = 50000;
   app.data.poolindex = 0;
   app.data.users = new Object ();
   app.data.emails = new Object ();
   app.data.cookies = new Object ();
   for (var i=0; i<app.data.poolsize; i++) {
      app.data.users[i] = "user"+i;
      app.data.emails[i] = "user"+i+"@somewhereintheworld.org";
      app.data.cookies[i] = i + "-session-key-for-the-application-server";
   }
}

function poolUser (idx) {
   var obj = new Object ();
   obj.username = app.data.users[idx];
   obj.email = app.data.emails[idx];
   obj.cookie = app.data.cookies[idx];
   return obj;
}

function randomUser () {
   var idx = Math.floor (Math.random ()*app.data.poolsize);
   return poolUser(idx);
}

function nextUser () {
	return  poolUser(app.data.poolindex++);
}

function header () {
	res.write ( "<h1>Application benchmark</h1><form method=post action=loginx>" );
	res.write ("<a href=main>main</a> ");
	res.write ("<a href=print>print</a> ");
	res.write ("<a href=check>check</a> ");
	res.write ("<a href=logout>logout</a> ");
	res.write ("<a href=login>login</a> ");
	res.write ("<a href=loginx?x=1000>login1000</a> ");
	res.write ("<a href=loginx?x=2000>login2000</a> ");
	res.write ("<a href=loginx?x=3000>login3000</a> ");
	res.write ("<input name=x size=4><input type=submit value=login>");
	res.write ( "<hr>" );
}

