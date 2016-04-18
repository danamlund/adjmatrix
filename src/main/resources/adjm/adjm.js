/**
 * Adjacency matrix code visualizer
 * Dan Amlund Thomsen <dan@danamlund.dk>
 * BSD license (2 clause)
 *
 * http://danamlund.dk/adjmatrix
 */

var state = {};

function main() {
    state.datas = data;
    state.data = first(state.datas);
    state.canvas = document.getElementById("canvas");
    state.canvasOverlay = document.getElementById("canvasOverlay");
    state.scale = 1;
    state.pixelSize = 0;
    state.filterText = "";
    state.zoomSelected = null;
    state.ordering = first(state.data.orderings);
    state.selected = state.ordering;
    state.colors = first(state.data.colors);

    window.addEventListener('resize', resizeWindow, false);
    state.canvasOverlay.addEventListener('mousemove', adjMatrixMouseMove);
    state.canvasOverlay.addEventListener('mouseout', adjMatrixMouseOut);

    { // zoom in adjacency matrix
        var downPoint = null;
        var getPoint = function(e) {
            var rect = state.canvas.getBoundingClientRect();
            var x = parseInt(e.clientX - rect.left);
            var y = parseInt(e.clientY - rect.top);
            
            if (x >= 0 && x < state.canvas.width && y >= 0 && y < state.canvas.height) {
                return {"x" : parseInt(x / state.scale), 
                        "y" : parseInt(y / state.scale) };
            } else {
                return null;
            }
        }
        state.canvasOverlay.addEventListener('mousedown', function(e) {
            downPoint = getPoint(e);
        });
        var prevMovePoint = null;
        state.canvasOverlay.addEventListener('mousemove', function(e) {
            var p = getPoint(e);
            if (p != prevMovePoint) {
                if (downPoint) {
                    drawSelection(downPoint, getPoint(e));
                }
                if (state.selected.length > 1) {
                    highlightPoint(p, !downPoint);
                }
            }
            prevMovePoint = p;
        });
        state.canvasOverlay.addEventListener('mouseup', function(e) {
            prevMovePoint = null;
            var upPoint = getPoint(e);
            if (downPoint !== null && upPoint !== null) {
                zoom(downPoint.x, downPoint.y, upPoint.x, upPoint.y);
            }
            downPoint = null;
        });
        var mousewheel = function(e) {
            var p = getPoint(e);
            if (p !== null) {
                var delta = e.wheelDelta ? e.wheelDelta/40 : (e.detail ? -e.detail : 0);
	        if (delta) {
                    var oldSize = state.selected.length;
                    var zoomAmount = 0.8;
                    if (delta > 0) { // zoom in
                        var newSize = oldSize <= 3 ? 1 : zoomAmount * oldSize;
                        if (newSize != state.selected.length) {
                            var delta = state.selected.length - newSize;
                            zoomin(p, delta);
                        }
                    } else { // zoom out
                        var newSize = Math.min(state.data.nodes.length, 
                                               Math.ceil(oldSize / zoomAmount));
                        if (newSize != state.selected.length) {
                            var delta = newSize - state.selected.length;
                            zoomout(delta);
                        }
                    }
                }
	        return e.preventDefault() && false;
            }
        };
        state.canvasOverlay.addEventListener('DOMMouseScroll', mousewheel, false);
        state.canvasOverlay.addEventListener('mousewheel', mousewheel, false);
    }

    {
        var groupingButtons = "Group by: "
        var firstGrouping = true;
        for (groupingKey in state.datas) {
            var id = "groupby_" + groupingKey;
            groupingButtons += "<input type=\"radio\" onchange=\"groupBy('" + groupingKey + "');\" "
                + "name=\"groupby\" id=\"" + id + "\" " 
                + (firstGrouping ? "checked=\"checked\"" : "") + ">"
                + "<label for=\"" + id + "\">" + groupingKey + "</label>";
            firstGrouping = false;
        }
        document.getElementById("groupings").innerHTML = groupingButtons;
    }

    document.getElementById("filterButton").addEventListener('click', filter, false);
    document.getElementById('filterText').onkeypress = function(e) {
        if (!e) e = window.event;
        var keyCode = e.keyCode || e.which;
        if (keyCode == '13') {
            filter();
        }
    };

    document.getElementById("addUses").addEventListener('click', function () { 
        addConnected([state.data.edgesFrom]); }, false);
    document.getElementById("addUsed").addEventListener('click', function () { 
        addConnected([state.data.edgesTo]); }, false);
    document.getElementById("addConnected").addEventListener('click', function () { 
        addConnected([state.data.edgesFrom, state.data.edgesTo]); }, false);

    populateOptions();

    var optionsRect = document.getElementById("options").getBoundingClientRect();;
    state.optionsHeight = optionsRect.height + 20;

    document.getElementById("setPixelSize").addEventListener('click', setPixelSize, false);

    document.getElementById("reset").addEventListener('click', reset, false);

    document.getElementById("populateTable").addEventListener("click", function () {
        var tableString = "";
        tableString += "<tr><td width=20></td><td>Uses</td><td>Used by</td><td>Name</td>";
        for (key in state.data.nodesData) {
            tableString += "<td>" + key + "</td>";
        }
        tableString += "</tr>\n";

        for (var i = 0; i < Math.min(1000, state.selected.length); i++) {
            var s = state.selected[i];
            var node = state.data.nodes[s];
            tableString += "<tr><td bgcolor=\"" + state.colors[s] + "\"></td>"
                + "<td>" + state.data.edgesFrom[s].length + "</td>"
                + "<td>" + state.data.edgesTo[s].length + "</td>"
                + "<td>" + node + "</td>";
            
            for (key in state.data.nodesData) {
                tableString += "<td>" + state.data.nodesData[key][s] + "</td>";
            }
            tableString += "</tr>\n";
        }
        document.getElementById("table").innerHTML = tableString;
    }, false);
    document.getElementById("clearTable").addEventListener("click", function () {
        document.getElementById("table").innerHTML = "";
    }, false);

    resizeWindow();
}

function groupBy(groupKey) {
    state.data = state.datas[groupKey];

    state.filterText = "";
    state.zoomSelected = null;
    state.ordering = first(state.data.orderings);
    state.selected = state.ordering;
    state.colors = first(state.data.colors);
    
    populateOptions();

    document.getElementById("table").innerHTML = "";

    resizeWindow();
}

function populateOptions() {
    var orderingButtons = "Order by: "
    var firstOrdering = true;
    for (orderingKey in state.data.orderings) {
        var id = "orderby_" + orderingKey;
        orderingButtons += "<input type=\"radio\" onchange=\"orderBy('" + orderingKey + "');\" "
                         + "name=\"orderby\" id=\"" + id + "\" " 
                         + (firstOrdering ? "checked=\"checked\"" : "") + ">"
                         + "<label for=\"" + id + "\">" + orderingKey + "</label>";
        firstOrdering = false;
    }
    document.getElementById("orderings").innerHTML = orderingButtons;

    var coloringButtons = "Color by: "
    var firstColoring = true;
    for (coloringKey in state.data.colors) {
        var id = "colorby_" + coloringKey;
        coloringButtons += "<input type=\"radio\" onchange=\"colorBy('" + coloringKey + "');\" "
                         + "name=\"colorby\" id=\"" + id + "\" " 
                         + (firstColoring ? "checked=\"checked\"" : "") + ">"
                         + "<label for=\"" + id + "\">" + coloringKey + "</label>";
        firstColoring = false;
    }
    document.getElementById("colorings").innerHTML = coloringButtons;
}

function setPixelSize() {
    state.pixelSize = document.getElementById("pixelSize").value;
    resizeWindow();
}

function first(o) {
    for (key in o) {
        return o[key];
    }
}

function orderBy(orderKey) {
    state.zoomSelected = null;
    state.ordering = state.data.orderings[orderKey];
    updateSelection();
}

function colorBy(colorKey) {
    state.colors = state.data.colors[colorKey];
    updateSelection();
}

function filter() {
    var newFilterText = document.getElementById("filterText").value;
    state.filterText = newFilterText;
    updateSelection();
}

function addConnected(edgesss) {
    state.filterText = null;
    var selectedContains = new Array(state.data.nodes.length);
    for (var i = 0; i < state.selected.length; i++) {
        selectedContains[state.selected[i]] = true;
    }

    var newSelected = [];
    for (var i = 0; i < state.selected.length; i++) {
        var node = state.selected[i];
        newSelected.push(node);
        for (var k = 0; k < edgesss.length; k++) {
            var edgess = edgesss[k];
            var edges = edgess[node];
            for (var j = 0; j < edges.length; j++) {
                var connection = edges[j];
                if (!selectedContains[connection]) {
                    newSelected.push(connection);
                    selectedContains[connection] = true;
                }
            }
        }
    }
    state.zoomSelected = newSelected;
    updateSelection();
}

function zoom(x1, y1, x2, y2) {
    var minX = Math.min(x1, x2);
    var maxX = Math.max(x1, x2);
    var minY = Math.min(y1, y2);
    var maxY = Math.max(y1, y2);
    
    var newSelected = [];
    for (var i = 0; i < state.selected.length; i++) {
        if ((i >= minX && i <= maxX)
            || i >= minY && i <= maxY) {
            newSelected.push(state.selected[i]);
        }
    }
    state.zoomSelected = newSelected;
    updateSelection();
}

function zoomout(amount) {
    if (state.zoomSelected !== null) {
        var zoomSelectedContains = new Array(state.zoomSelected.length);
        for (var i = 0; i < state.zoomSelected.length; i++) {
            zoomSelectedContains[state.zoomSelected[i]] = i;
        }
        
        var maxSize = state.ordering.length - 1;
        { // First add nodes between the zoom selection.
            var from = state.ordering.indexOf(state.zoomSelected[0]);
            var to = state.ordering.indexOf(state.zoomSelected[state.zoomSelected.length - 1]);
        
            var newZoomSelected = [];
            for (var i = from; i <= to; i++) {
                var node = state.ordering[i];
                if (zoomSelectedContains[node] !== "undefined") {
                    newZoomSelected.push(node);
                } else {
                    if (amount > 0) {
                        amount--;
                        newZoomSelected.push(node);
                    }
                }
            }
            state.zoomSelected = newZoomSelected;
        }
        
        { // Then add nodes to the ends
            var from = state.ordering.indexOf(state.zoomSelected[0]);
            var to = state.ordering.indexOf(state.zoomSelected[state.zoomSelected.length - 1]);
            
            while (amount > 0 && (from > 0 || to < maxSize)) {
                if (from > 0) {
                    from--;
                    state.zoomSelected.unshift(state.ordering[from]);
                    amount--;
                }
                if (amount > 0 && to < maxSize) {
                    to++;
                    state.zoomSelected.push(state.ordering[to]);
                    amount--;
                }
            }
        }
        updateSelection();
    }
}

function zoomin(p, amount) {
    var pDists = [];
    for (var i = 0; i < state.selected.length; i++) {
        var pDist = Math.min(Math.abs(i - p.x), Math.abs(i - p.y));
        pDists.push({"i" : i, "dist" : pDist});
    }
    
    pDists.sort(function (a, b) { return a.dist < b.dist ? 1 : (a.dist == b.dist ? 0 : -1) });

    var removeI = new Array(state.selected.length);
    for (var j = 0; j < amount && j < pDists.length; j++) {
        removeI[pDists[j].i] = true;
    }
    
    var newZoomSelected = [];
    for (var i = 0; i < state.selected.length; i++) {
        if (!removeI[i]) {
            newZoomSelected.push(state.selected[i]);
        }
    }
    
    state.zoomSelected = newZoomSelected;
    updateSelection();
}

function reset() {
    state.zoomSelected = null;
    document.getElementById("filterText").value = "";
    state.filterText = null;
    updateSelection();
}

function updateSelection() {
    if (state.zoomSelected !== null) {
        state.selected = state.zoomSelected;
    } else if (state.filterText !== null) {
        state.selected = [];
        for (var i = 0; i < state.ordering.length; i++) {
            if (matches(state.data.nodes[state.ordering[i]], state.filterText)) {
                state.selected.push(state.ordering[i]);
            }
        }
    } else {
        state.selected = state.ordering;
    }
    resizeWindow();
}

function matches(haystack, needle) {
    return haystack.toLowerCase().indexOf(needle.toLowerCase()) !== -1;
}

function getArrayIndexOfer(a, len) {
    if (typeof(len) === "undefined") {
        len = a.length;
    }
    var indexOfer = new Array(len);
    for (var i = 0; i < a.length; i++) {
        indexOfer[a[i]] = i;
    }
    return indexOfer
}

function resizeWindow() {
    drawAdjacencyMatrix();
    adjMatrixMouseOut(null);
}

function drawSelection(p1, p2) {
    var canvas = state.canvasOverlay;
    var ctx = canvas.getContext("2d");
    ctx.clearRect(0, 0, canvas.width, canvas.height);
    var x = parseInt(Math.min(p1.x, p2.x)) * state.scale;
    var y = parseInt(Math.min(p1.y, p2.y)) * state.scale;
    var width = (parseInt(Math.abs(p1.x - p2.x)) + 1) * state.scale;
    var height = (parseInt(Math.abs(p1.y - p2.y)) + 1) * state.scale;
    ctx.strokeStyle = "black";
    ctx.lineWidth = 3;
    ctx.strokeRect(x, y, width, height);
    ctx.fillStyle = "rgba(0,0,255,0.5)";
    ctx.fillRect(x, y, width, height);
}

function highlightPoint(p, clear) {
    var canvas = state.canvasOverlay;
    var ctx = canvas.getContext("2d");
    if (clear) {
        ctx.clearRect(0, 0, canvas.width, canvas.height);
    }
    var x = p.x * state.scale;
    var y = p.y * state.scale;
    var pixelSize = getPixelSize();
    ctx.fillStyle = "rgba(0,0,255,0.2)";
    ctx.fillRect(x, 0, pixelSize, canvas.height);
    ctx.fillRect(0, y, canvas.width, pixelSize);
}

function getPixelSize() {
    return state.scale <= 0.5 ? 0.5 : state.scale;
}
function drawAdjacencyMatrix() {
    var ctx = state.canvas.getContext("2d");

    var canvasSize = Math.min(window.innerWidth, 
                              window.innerHeight - state.optionsHeight) * 0.9;
    state.scale = canvasSize / state.selected.length;

    var size = state.selected.length;
    state.canvas.width = size * state.scale;
    state.canvas.height = state.canvas.width;

    var canvasRect = state.canvas.getBoundingClientRect();
    state.canvasOverlay.style.top = canvasRect.top + window.scrollY;
    state.canvasOverlay.style.left = canvasRect.left;
    state.canvasOverlay.width = state.canvas.width;
    state.canvasOverlay.height = state.canvas.height;
    var overlayCtx = state.canvasOverlay.getContext("2d");
    overlayCtx.fillStyle = "rgba(0,0,0,0.0)"
    overlayCtx.fillRect(0, 0, state.canvasOverlay.width, state.canvasOverlay.height);

    if (state.pixelSize == 0) {
        ctx.globalAlpha = 1.0;
    } else {
        ctx.globalAlpha = 0.3;
    }

    var putPixel = function pixel(x, y, color) {
        pixelSize = getPixelSize();
        ctx.fillStyle = color;
        if (state.pixelSize == 0) {
            ctx.fillRect(x, y, pixelSize, pixelSize);
        } else {
            ctx.beginPath();
            ctx.arc(x, y, pixelSize * state.pixelSize, 0, 2 * Math.PI, false);
            ctx.fill();
        }
    }
    
    ctx.strokeStyle = "black";
    ctx.strokeRect(0, 0, state.canvas.width, state.canvas.height);

    document.getElementById("info").innerHTML = 
        "Showing " + size + "/" + state.data.nodes.length + " nodes";
    
    var selectedIndexOf = getArrayIndexOfer(state.selected, state.data.nodes.length);

    for (var i = 0; i < state.selected.length; i++) {
        var from = state.selected[i];
        var edges = state.data.edgesFrom[from];
        for (var j = 0; j < edges.length; j++) {
            var to = edges[j];
            if (selectedIndexOf[to] !== "undefined") {
                var y = i;
                var x = selectedIndexOf[to];
                putPixel(x * state.scale, y * state.scale, state.colors[from]);
            }
        }
    }

    for (var i = 0; i < state.selected.length; i++) {
        putPixel(i * state.scale, i * state.scale, state.colors[state.selected[i]]);
    }
}

function adjMatrixMouseMove(e) {
    var rect = state.canvas.getBoundingClientRect();
    var x = parseInt(e.clientX - rect.left);
    var y = parseInt(e.clientY - rect.top);

    var inArray = function(arr, e) {
        for (var i = 0; i < arr.length; i++) {
            if (e == arr[i]) {
                return true;
            }
        }
        return false;
    }
    
    if (x >= 0 && x < state.canvas.width && y >= 0 && y < state.canvas.height) {
        var from = state.selected[parseInt(y / state.scale)];
        var to = state.selected[parseInt(x / state.scale)];
        if (from == to) {
            document.getElementById("tooltip").innerHTML = 
                state.data.nodes[from];
        } else {
            if (inArray(state.data.edgesFrom[from], to)) {
                document.getElementById("tooltip").innerHTML = 
                    state.data.nodes[from] + " uses " + state.data.nodes[to];
            } else {
                adjMatrixMouseOut(null);
            }
        }
    }
}

function adjMatrixMouseOut(e) {
    document.getElementById("tooltip").innerHTML = "&nbsp;";
}


