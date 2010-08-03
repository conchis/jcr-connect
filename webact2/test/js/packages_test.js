module("packages.js");

test("create", function () {
	var proto = {x: 22, y: 7, z: 12};
	var p1 = webact.create(proto, {z: 0, color: "green"});
	equals(p1.x, 22);
	equals(p1.z, 0);
	equals(p1.color, "green");
});

test("in_package", function () {
    webact.in_package("p1", function (p1) {
        p1.x = 22;
        p1.y = 10;
    });
    
    var p1 = webact.p1;
    ok(p1, "package created");
    equals(p1.x, 22);
    equals(p1.y, 10);
});

test("names", function () {
    webact.in_package("p2", function (p2) {
        p2.x = 8;
        p2.y = 4;
    });  
    
    same(webact.p2.names(), ["x", "y"]);
});

test("exports", function () {
    webact.in_package("p3", function (p3) {
        p3.x = 4;
        p3.y = 2;
    });  
    
    equals(webact.p3.exports(), "var x=webact.p3.x;var y=webact.p3.y");
    
    eval(webact.p3.exports());
    equals(x, 4);
    equals(y, 2);
});

test("imports", function () {
    webact.in_package("p4", function (p4) {
        p4.x = 0;
        p4.y = 1;
    });
    webact.in_package("p5", function (p5) {
        p5.z = 2;
    });
    
    eval(webact.imports("p4", "p5"));
    equals(x, 0);
    equals(y, 1); 
    equals(z, 2);
});
