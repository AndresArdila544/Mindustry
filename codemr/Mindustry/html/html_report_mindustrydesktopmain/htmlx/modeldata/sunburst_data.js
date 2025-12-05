function EQ_GET_DATA(){ 
	  var ret = {
"name": " Mindustry working set: {Mindustry.desktop.main}", "value":99152, 
"prmetrics":{"5":1,"6":1,"7":1,"8":1,"9":3,"10":1,"11":2},
"prmetricvalues":{"5":1,"6":7,"7":3,"8":793,"9":29,"10":2,"11":104},
"children": [ {
"name": "mindustry.desktop", "key": "e;", "value":252, 
"pmetrics":{"4":2,"12":1,"13":1,"14":1,"3":1,"1":1,"0":1,"6":1,"8":2,"2":1,"15":1,"16":1},
"pmetricvalues":{"4":2,"12":1,"13":0,"14":1,"3":1,"1":1,"0":1,"17":0.0,"6":1,"18":0.0,"8":252,"19":1.0,"2":1,"15":0,"16":66},
"children":[
{
"name": "DesktopLauncher","key": "e.","value":252, 
"metrics":{"20":5,"21":1,"22":1,"23":5,"24":2,"25":1,"26":2,"0":5,"27":1,"28":4,"29":5,"30":3,"31":1,"32":2,"33":4,"34":2,"35":1,"36":3,"16":3,"37":2,"4":2,"8":2,"2":5,"3":2,"1":4},
"metricvalues":{"20":38,"21":0,"22":4,"23":1.846,"24":247,"25":0,"26":3,"0":5,"27":13,"28":161,"29":0.985,"30":0.8,"31":1,"32":0.675,"33":21,"34":56,"35":3,"36":17,"16":66,"37":8,"4":2,"8":252,"2":5,"3":2,"1":4}
}
]
},{
"name": "mindustry.desktop.steam", "key": "sX", "value":541, 
"pmetrics":{"4":2,"12":1,"13":1,"14":2,"3":1,"1":1,"0":1,"6":2,"8":2,"2":1,"15":1,"16":1},
"pmetricvalues":{"4":2,"12":4,"13":0,"14":6,"3":1,"1":1,"0":1,"17":0.2,"6":6,"18":0.0,"8":541,"19":0.8,"2":1,"15":1,"16":125},
"children":[
{
"name": "SStats","key": "s1","value":26, 
"metrics":{"20":2,"21":1,"22":1,"23":1,"24":1,"25":1,"26":1,"0":2,"27":1,"28":1,"29":4,"30":4,"31":1,"32":1,"33":1,"34":1,"35":1,"36":1,"16":1,"37":1,"4":1,"8":1,"2":2,"3":1,"1":1},
"metricvalues":{"20":7,"21":0,"22":3,"23":0.0,"24":22,"25":0,"26":1,"0":2,"27":4,"28":41,"29":0.833,"30":1.0,"31":0,"32":0.437,"33":2,"34":6,"35":0,"36":5,"16":7,"37":0,"4":1,"8":26,"2":2,"3":1,"1":1}
},
{
"name": "SWorkshop","key": "s4","value":214, 
"metrics":{"20":4,"21":1,"22":1,"23":1,"24":2,"25":1,"26":1,"0":5,"27":1,"28":2,"29":1,"30":3,"31":1,"32":3,"33":2,"34":2,"35":1,"36":4,"16":2,"37":1,"4":2,"8":2,"2":5,"3":3,"1":2},
"metricvalues":{"20":29,"21":0,"22":5,"23":0.0,"24":208,"25":0,"26":1,"0":5,"27":16,"28":99,"29":0.6,"30":0.8,"31":0,"32":0.745,"33":8,"34":55,"35":0,"36":21,"16":40,"37":0,"4":2,"8":214,"2":5,"3":3,"1":2}
},
{
"name": "SUser","key": "s2","value":2, 
"metrics":{"20":1,"21":1,"22":1,"23":1,"24":1,"25":1,"26":1,"0":1,"27":1,"28":1,"29":1,"30":1,"31":1,"32":1,"33":1,"34":1,"35":1,"36":1,"16":1,"37":1,"4":1,"8":1,"2":1,"3":1,"1":1},
"metricvalues":{"20":1,"21":0,"22":1,"23":0.0,"24":0,"25":0,"26":1,"0":1,"27":0,"28":0,"29":0.0,"30":0.0,"31":0,"32":0.0,"33":0,"34":0,"35":0,"36":1,"16":0,"37":0,"4":1,"8":2,"2":1,"3":1,"1":1}
},
{
"name": "SVars","key": "s3","value":6, 
"metrics":{"20":1,"21":1,"22":1,"23":1,"24":1,"25":1,"26":1,"0":1,"27":1,"28":1,"29":1,"30":1,"31":1,"32":1,"33":1,"34":1,"35":1,"36":1,"16":1,"37":1,"4":1,"8":1,"2":1,"3":1,"1":1},
"metricvalues":{"20":4,"21":0,"22":0,"23":0.0,"24":0,"25":0,"26":1,"0":1,"27":0,"28":0,"29":0.0,"30":0.0,"31":5,"32":0.0,"33":4,"34":0,"35":0,"36":0,"16":0,"37":0,"4":1,"8":6,"2":1,"3":1,"1":1}
},
{
"name": "SNet","key": "sY","value":293, 
"metrics":{"20":5,"21":1,"22":1,"23":1,"24":2,"25":1,"26":1,"0":5,"27":2,"28":4,"29":1,"30":4,"31":1,"32":4,"33":4,"34":2,"35":1,"36":4,"16":3,"37":1,"4":2,"8":2,"2":5,"3":4,"1":4},
"metricvalues":{"20":45,"21":0,"22":15,"23":0.0,"24":249,"25":0,"26":1,"0":5,"27":23,"28":194,"29":0.521,"30":0.879,"31":0,"32":0.839,"33":21,"34":90,"35":0,"36":24,"16":68,"37":0,"4":2,"8":293,"2":5,"3":4,"1":4}
}
]
}]
 }
;
return ret;
}
var EQ_METRIC_MAP = {};
EQ_METRIC_MAP["C3"] =0;
EQ_METRIC_MAP["Complexity"] =1;
EQ_METRIC_MAP["Coupling"] =2;
EQ_METRIC_MAP["Lack of Cohesion"] =3;
EQ_METRIC_MAP["Size"] =4;
EQ_METRIC_MAP["Number of Highly Problematic Classes"] =5;
EQ_METRIC_MAP["Number of Entities"] =6;
EQ_METRIC_MAP["Number of Problematic Classes"] =7;
EQ_METRIC_MAP["Class Lines of Code"] =8;
EQ_METRIC_MAP["Number of External Packages"] =9;
EQ_METRIC_MAP["Number of Packages"] =10;
EQ_METRIC_MAP["Number of External Entities"] =11;
EQ_METRIC_MAP["Efferent Coupling"] =12;
EQ_METRIC_MAP["Number of Interfaces"] =13;
EQ_METRIC_MAP["Number of Classes"] =14;
EQ_METRIC_MAP["Afferent Coupling"] =15;
EQ_METRIC_MAP["Weighted Method Count"] =16;
EQ_METRIC_MAP["Normalized Distance"] =17;
EQ_METRIC_MAP["Abstractness"] =18;
EQ_METRIC_MAP["Instability"] =19;
EQ_METRIC_MAP["Coupling Between Object Classes"] =20;
EQ_METRIC_MAP["Access to Foreign Data"] =21;
EQ_METRIC_MAP["Number of Fields"] =22;
EQ_METRIC_MAP["Specialization Index"] =23;
EQ_METRIC_MAP["Class-Methods Lines of Code"] =24;
EQ_METRIC_MAP["Number of Children"] =25;
EQ_METRIC_MAP["Depth of Inheritance Tree"] =26;
EQ_METRIC_MAP["Number of Methods"] =27;
EQ_METRIC_MAP["Response For a Class"] =28;
EQ_METRIC_MAP["Lack of Tight Class Cohesion"] =29;
EQ_METRIC_MAP["Lack of Cohesion of Methods"] =30;
EQ_METRIC_MAP["Number of Static Fields"] =31;
EQ_METRIC_MAP["Lack of Cohesion Among Methods(1-CAM)"] =32;
EQ_METRIC_MAP["CBO App"] =33;
EQ_METRIC_MAP["Simple Response For a Class"] =34;
EQ_METRIC_MAP["Number of Static Methods"] =35;
EQ_METRIC_MAP["CBO Lib"] =36;
EQ_METRIC_MAP["Number of Overridden Methods"] =37;
var EQ_SELECTED_CLASS_METRIC 		= "Coupling";
var EQ_SELECTED_PACKAGE_METRIC 	= "Coupling";
var EQ_SELECTED_PROJECT_METRIC 	= "Class Lines of Code";
var EQ_CLASS_METRIC_INDEX 	= EQ_METRIC_MAP[EQ_SELECTED_CLASS_METRIC];
var EQ_PACKAGE_METRIC_INDEX	= EQ_METRIC_MAP[EQ_SELECTED_PACKAGE_METRIC];
var EQ_PROJECT_METRIC_INDEX 	= EQ_METRIC_MAP[EQ_SELECTED_PROJECT_METRIC];
var EQ_COLOR_OF_LEVELS = ["#1F77B4","#007F24","#62BF18","#FFC800","#FF5B13","#E50000"];
var EQ_CLASS_METRICS = ["C3","Complexity","Coupling","Lack of Cohesion","Size","Class Lines of Code","Weighted Method Count","Coupling Between Object Classes","Access to Foreign Data","Number of Fields","Specialization Index","Class-Methods Lines of Code","Number of Children","Depth of Inheritance Tree","Number of Methods","Response For a Class","Lack of Tight Class Cohesion","Lack of Cohesion of Methods","Number of Static Fields","Lack of Cohesion Among Methods(1-CAM)","CBO App","Simple Response For a Class","Number of Static Methods","CBO Lib","Number of Overridden Methods"];
var EQ_PACKAGE_METRICS = ["C3","Complexity","Coupling","Lack of Cohesion","Size","Number of Entities","Class Lines of Code","Efferent Coupling","Number of Interfaces","Number of Classes","Afferent Coupling","Weighted Method Count","Normalized Distance","Abstractness","Instability"];
var EQ_PROJECT_METRICS = ["Number of Highly Problematic Classes","Number of Entities","Number of Problematic Classes","Class Lines of Code","Number of External Packages","Number of Packages","Number of External Entities"];
function EQ_GET_COLOR(d) {
if(d.metrics)
return EQ_COLOR_OF_LEVELS[d.metrics[EQ_CLASS_METRIC_INDEX]];
if(d.pmetrics)
return EQ_COLOR_OF_LEVELS[d.pmetrics[EQ_PACKAGE_METRIC_INDEX]];
if(d.prmetrics)
return EQ_COLOR_OF_LEVELS[d.prmetrics[EQ_PROJECT_METRIC_INDEX]];
return EQ_COLOR_OF_LEVELS[0];
}
