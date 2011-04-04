
function main_action () {
   if (this.checkAccess ()==false)  return;
	this.renderSkin("header");
   var arr = this.getGroups ();
	for (var key in arr) {
	   var param = new Object ();
	   param.name = key;
	   param.connection = group.getConnection (key);
	   param.members = this.renderMembers (key);
	   param.config  = this.renderConfig (key);
	   param.content = this.renderContent (key);
	   this.renderSkin("group", param);
   }
}

function connect_action () {
   if (this.checkAccess ()==false)  return;
	group.connect (req.data.name);
	res.message = "connected to group " + req.data.name;
	res.redirect (this.href ("main"));
}

function disconnect_action () {
   if (this.checkAccess ()==false)  return;
	group.disconnect (req.data.name);
	res.message = "disconnected from group " + req.data.name;
	res.redirect (this.href ("main"));
}

function reconnect_action () {
   if (this.checkAccess ()==false)  return;
	group.reconnect (req.data.name);
	res.message = "reconnected to group " + req.data.name;
	res.redirect (this.href ("main"));
}

function reset_action () {
   if (this.checkAccess ()==false)  return;
	group.reset (req.data.name);
	res.message = "reset group " + req.data.name;
	res.redirect (this.href ("main"));
}

function destroy_action () {
   if (this.checkAccess ()==false)  return;
	group.destroy (req.data.name);
	res.message = "destroyed group " + req.data.name;
	res.redirect (this.href ("main"));
}


