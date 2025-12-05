function EQ_GET_DATA(){ 
	  var ret = {
"name": " Mindustry working set: {arc.extensions.profiling.main}", "value":68664, 
"prmetrics":{"5":1,"6":1,"7":1,"8":2,"9":1,"10":1,"11":1},
"prmetricvalues":{"5":0,"6":5,"7":2,"8":2140,"9":5,"10":1,"11":15},
"children": [ {
"name": "arc.profiling", "key": "gXr", "value":2140, 
"pmetrics":{"4":3,"12":1,"13":1,"14":1,"3":1,"1":2,"0":2,"6":1,"8":3,"2":1,"15":1,"16":2},
"pmetricvalues":{"4":3,"12":4,"13":1,"14":4,"3":1,"1":2,"0":2,"17":0.4,"6":5,"18":0.4,"8":2140,"19":1.0,"2":1,"15":0,"16":421},
"children":[
{
"name": "GLErrorListener","key": "gXv","value":28, 
"metrics":{"20":1,"21":1,"22":1,"23":1,"24":1,"25":1,"26":1,"0":1,"27":1,"28":1,"29":5,"30":1,"31":1,"32":1,"33":1,"34":1,"35":1,"36":1,"37":1,"4":1,"8":1,"2":1,"3":1,"16":1,"1":1},
"metricvalues":{"20":0,"21":0,"22":0,"23":0.0,"24":2,"25":0,"26":1,"0":1,"27":1,"28":6,"29":1.0,"30":0.0,"31":2,"32":0.25,"33":0,"34":4,"35":1,"36":0,"37":0,"4":1,"8":28,"2":1,"3":1,"16":2,"1":1}
},
{
"name": "GLInterceptor","key": "gXC","value":25, 
"metrics":{"20":1,"21":1,"22":1,"23":1,"24":1,"25":2,"26":1,"0":1,"27":1,"28":1,"29":5,"30":4,"31":1,"32":1,"33":1,"34":1,"35":1,"36":1,"16":1,"37":1,"4":1,"8":1,"2":1,"3":1,"1":1},
"metricvalues":{"20":2,"21":0,"22":7,"23":0.0,"24":17,"25":2,"26":1,"0":1,"27":2,"28":6,"29":1.0,"30":1.0,"31":0,"32":0.444,"33":2,"34":1,"35":1,"36":0,"16":8,"37":0,"4":1,"8":25,"2":1,"3":1,"1":1}
},
{
"name": "GLProfiler","key": "gXE","value":47, 
"metrics":{"20":2,"21":1,"22":1,"23":1,"24":1,"25":1,"26":1,"0":2,"27":1,"28":1,"29":1,"30":3,"31":1,"32":2,"33":2,"34":1,"35":1,"36":1,"16":1,"37":1,"4":1,"8":1,"2":2,"3":2,"1":1},
"metricvalues":{"20":7,"21":0,"22":4,"23":0.0,"24":42,"25":0,"26":1,"0":2,"27":13,"28":21,"29":0.449,"30":0.708,"31":0,"32":0.615,"33":7,"34":5,"35":0,"36":0,"16":18,"37":0,"4":1,"8":47,"2":2,"3":2,"1":1}
},
{
"name": "GL20Interceptor","key": "gXs","value":801, 
"metrics":{"20":1,"21":1,"22":1,"23":1,"24":3,"25":1,"26":2,"0":5,"27":5,"28":5,"29":1,"30":1,"31":1,"32":3,"33":1,"34":4,"35":1,"36":1,"16":5,"37":1,"4":5,"8":3,"2":1,"3":3,"1":5},
"metricvalues":{"20":5,"21":1,"22":1,"23":0.0,"24":799,"25":0,"26":2,"0":5,"27":153,"28":314,"29":0.0,"30":0.0,"31":0,"32":0.759,"33":5,"34":158,"35":0,"36":0,"16":155,"37":0,"4":5,"8":801,"2":1,"3":3,"1":5}
},
{
"name": "GL30Interceptor","key": "gXu","value":1239, 
"metrics":{"20":2,"21":1,"22":1,"23":1,"24":4,"25":1,"26":2,"0":5,"27":5,"28":5,"29":1,"30":1,"31":1,"32":3,"33":2,"34":5,"35":1,"36":1,"16":5,"37":1,"4":5,"8":4,"2":2,"3":3,"1":5},
"metricvalues":{"20":6,"21":1,"22":1,"23":0.0,"24":1237,"25":0,"26":2,"0":5,"27":236,"28":480,"29":0.0,"30":0.0,"31":0,"32":0.775,"33":6,"34":241,"35":0,"36":0,"16":238,"37":0,"4":5,"8":1239,"2":2,"3":3,"1":5}
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
