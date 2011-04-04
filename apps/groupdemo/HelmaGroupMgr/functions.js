function checkAccess () {
   if (req.username==null || req.password==null || req.username!=getProperty("groupadmin.username") || req.password!=getProperty("groupadmin.password")) {
      res.reset ();
      res.status = 401;
      res.realm = "helmagroups";
      res.write ("Authorization required.");
      return false;
   } else {
      return true;
   }
}


function href_macro (param) {
   return this.href (param.action);
}


/**
  * lists all configured groups (including those that are disconnected at the moment)
  */
function getGroups () {
   var props = app.__app__.getProperties ();
   var e = props.keys ();
   var arr = [];
   while (e.hasMoreElements ()) {
      var key = e.nextElement ();
      if (0==key.indexOf("group.")) {
         var pArr = key.split(".");
         if (pArr.length == 2)
            arr.push(pArr[1]);
      }
   }
   return arr;
}



function renderGroup (name) {
   var param = new Object ();
	param.name = name;
	param.connection = groups.getConnection (name);
	param.members = this.renderMembers (name);
	param.memberApps = this.renderMemberApps (name);
	param.config  = this.renderConfig (name);
	param.content = this.renderContent (name);
	this.renderSkin("group", param);
}


function renderContent (name) {
   if (req.data.content=="true" && req.data.name==name) {
      return encode (groups.getContent (name).trim ());
   } else if (req.data.fullcontent=="true" && req.data.name==name) {
      return encode (groups.getFullContent (name).trim ());
   } else {
      return groups.count (name) + " object(s) in the group";
   }
}

function renderConfig (name) {
   if (req.data.config=="true" && req.data.name==name) {
      var str = groups.getFullConfig (name);
   } else {
      var str = groups.getConfig (name);
   }
   var str = str.trim ();
   var reg = new RegExp ("\n");
   reg.global = true;
   var str = str.replace (reg, ", ");
   var reg = new RegExp (", , ");
   reg.global = true;
   var str = str.replace (reg, "<br>");
   return str;
}

function renderMembers (name) {
   var str = "";
   var arr = groups.listMembers (name);
   for (var i=0; i<arr.length; i++) {
      str += "[" + arr[i]  + "]";
   }
   return str;
}


function renderMemberApps (name) {
   res.push();
   var arr = groups.listMemberApps(name);
   for (var i=0; i<arr.length; i++) {
      res.write("[" + arr[i]  + "]");
   }
   return res.pop();
}

