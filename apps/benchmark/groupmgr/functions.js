
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
   var arr = new Array ();
   while (e.hasMoreElements ()) {
      var key = e.nextElement ();
      if (0==key.indexOf("group.")) {
         var lidx = key.lastIndexOf(".");
         if (lidx==5)
            lidx = key.length;
         var name = key.substring (6, lidx);
         arr[name] = name;
      }
   }
   return arr;
}

function renderContent (name) {
   if (req.data.content=="true" && req.data.name==name) {
      return encode (group.getContent (name).trim ());
   } else if (req.data.fullcontent=="true" && req.data.name==name) {
      return encode (group.getFullContent (name).trim ());
   } else {
      return group.count (name) + " object(s) in the group";
   }
}

function renderConfig (name) {
   if (req.data.config=="true" && req.data.name==name) {
      var str = group.getFullConfig (name);
   } else {
      var str = group.getConfig (name);
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
   if (req.data.details=="true" && req.data.name==name) {
      // call any function, it will most certainly throw an error
      // but we just need the names of the clients, not the results
      var arr = group.getRemote (name).someBogusFunction ();
      for (var i=0; i<arr.length; i++) {
         str += "[" + arr[i].app + "@" + arr[i].host + "]";
         if (i<arr.length-1)
            str += "<br/>";
      }
   } else {
      var arr = group.getMembers (name);
      for (var i=0; i<arr.length; i++) {
         str += "[" + arr[i]  + "]";
      }
   }
   return str;
}

