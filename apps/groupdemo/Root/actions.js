function test_action () {
   this.groupmgr.renderSkin("header");
   this.groupmgr.renderGroup("sessions");
	this.renderSkin("demoButtons");
   res.write ("<pre>");
   // testcode here ...
   res.write("nothing to test");
}

function main_action () {
   this.groupmgr.renderSkin("header");
   this.groupmgr.renderGroup("sessions");
	this.renderSkin("demoButtons");
   res.write ("<pre>");
   res.encode (groups.getContent ("sessions"));
}

function addobjects_action () {
   var ct = groups.count("sessions");
   for (var i=ct; i<(ct+10); i++) {
		tmp = new GroupObject();
		tmp.set("username", "testusername"+i);
		tmp.set("onSince", new Date ());
		tmp.set("email", "testemail"+i+"@testserver.tld");
		tmp.set("sessionID", "sessionkey" + i);
		groups.get("sessions").set("sessionkey"+i, tmp);
	}
	res.message = "10 objects added";
	res.redirect(this.href("main"));
}

function removeobjects_action () {
   var i = 0;
   var arr = groups.get("sessions").listChildren();
   for (var i=0; i<arr.length; i++) {
      groups.get("sessions").set(arr[i], null);
      if (i>9) {
         break;
      }
   }
	res.message = "10 objects removed";
	res.redirect(this.href("main"));
}

function execute_action () {
   this.groupmgr.renderSkin("header");
   this.groupmgr.renderGroup("sessions");
	this.renderSkin("demoButtons");
   res.write ("<pre>");
	res.writeln("executed:");
	var re = groups.get("sessions").callFunction("testfunction", ["17", "4"]);
	for (var i=0; i<re.length; i++) {
	   res.writeln ("result [" + i + "] = " + re[i]);
   }
}

function testfunction (arg1, arg2) {
   return "a result from " + app.__app__.getName ()+ " at " + new Date ();
}

