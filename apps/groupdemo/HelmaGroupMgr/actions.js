
function main_action () {
   if (this.checkAccess ()==false)  return;
	this.renderSkin("header");
   var arr = this.getGroups();
	for (var i in arr) {
	   this.renderGroup(arr[i]);
   }
   return;
}

function connect_action () {
   if (this.checkAccess ()==false)  return;
	groups.connect (req.data.name);
	res.message = "connected to group " + req.data.name;
	res.redirect (this.href ("main"));
   return;
}

function disconnect_action () {
   if (this.checkAccess ()==false)  return;
	groups.disconnect (req.data.name);
	res.message = "disconnected from group " + req.data.name;
	res.redirect (this.href ("main"));
   return;
}

function reconnect_action () {
   if (this.checkAccess ()==false)  return;
	groups.reconnect (req.data.name);
	res.message = "reconnected to group " + req.data.name;
	res.redirect (this.href ("main"));
   return;
}

function reset_action () {
   if (this.checkAccess ()==false)  return;
	groups.reset (req.data.name);
	res.message = "reset group " + req.data.name;
	res.redirect (this.href ("main"));
   return;
}

function destroy_action () {
   if (this.checkAccess ()==false)  return;
	groups.destroy (req.data.name);
	res.message = "destroyed group " + req.data.name;
	res.redirect (this.href ("main"));
   return;
}

//	res.write ( "<a href=addobjects>add 10 objects</a> | ");
//	res.write ( "<a href=removeobjects>remove 10 objects</a> | ");
//	res.write ( "<a href=execute>call function</a> | ");
//	res.write ( "<a href=test>test</a>");
