;
(function() {
    function escapeId(id) {
        // replace all characters except numbers and letters
        // As soon as someone opens a bug that they have a problem with a name conflict
        // rethink the strategy
        return id.replace(/[^a-zA-Z0-9]/g,'-');
    }

    function getJobDiv(jobName) {
        return jQuery('#' + escapeId(jobName));
    }

    window.depview = {
        paper: jQuery("#paper"),
        colordep: '#FF0000', // red
        colorcopy: '#32CD32', // green
        init : function() {
            jsPlumb.importDefaults({
                Connector : ["StateMachine", { curviness: 10 }],// Straight, Flowchart, Straight, Bezier
                // default drag options
                DragOptions : {
                    cursor : 'pointer',
                    zIndex : 2000
                },
                // default to blue at one end and green at the other
                EndpointStyles : [ {
                    fillStyle : '#225588'
                }, {
                    fillStyle : '#558822'
                } ],
                // blue endpoints 7px; green endpoints 7px.
                Endpoints : [ [ "Dot", {
                    radius : 6
                } ], [ "Dot", {
                    radius : 6
                } ] ],

                // def for new connector (drag n' drop) 
                // - line 2px
                PaintStyle : {
                    lineWidth : 2,
                    strokeStyle : window.depview.colordep,
                    joinstyle:"round"},

                // the overlays to decorate each connection with. note that the
                // label overlay uses a function to generate the label text; in
                // this case it returns the 'labelText' member that we set on each
                // connection in the 'init' method below.
                ConnectionOverlays : [ [ "Arrow", {
                    location : 1.0,
                    foldback:0.5
                } ]
                ]

            });
            jQuery.getJSON('graph.json', function(data) {

                var top = 3;
                var space = 150;
                var xOverall = 0;

                var clusters = data["clusters"];
                // iterate clusters
                jQuery.each(clusters, function(i, cluster) {
                    jQuery.each(cluster.nodes, function(i,node) {
                        jQuery('<div><div class="ep"/><a href="' + node.url + '">' + node.name + '</a></div>').
                            addClass('window').
                            attr('id', escapeId(node.name)).
                            attr('data-jobname', node.fullName).
                            css('top', node.y + top).
                            css('left', node.x + xOverall).
                            appendTo(window.depview.paper);
                    })
                    top = top + cluster.vSize + space
                    // xOverall = xOverall + cluster.hSize + space
                });
                // definitions for drag/drop connections
                jQuery(".ep").each(function(idx, current) {
                    var p = jQuery(current).parent()
                    jsPlumb.makeSource(current, {
                        anchor : "Continuous",
                        parent: p
                    });
                })
                jsPlumb.makeTarget(jsPlumb.getSelector('.window'), {
                    anchor : "Continuous"
                });

                var edges = data["edges"];
                jQuery.each(edges, function(i, edge) {
                    from = getJobDiv(edge["from"]);
                    to = getJobDiv(edge["to"]);
                    // creates/defines the look and feel of the loaded connections: red="dep", green="copy"
                    var connection;
                    if("copy" == edge["type"]){
                        connection = jsPlumb.connect({ source : from, target : to, scope: edge["type"], paintStyle:{lineWidth : 2, strokeStyle: window.depview.colorcopy},
                                                       overlays:[[ "Label", { label: "copy", id: from+'.'+to } ]]
                                                    });
                    }else{
                        connection = jsPlumb.connect({ source : from, target : to, scope: edge["type"], paintStyle:{lineWidth : 2, strokeStyle: window.depview.colordep}});
                        // only allow deletion of "dep" connections
                        connection.bind("click", function(conn) {
                            var sourceJobName = conn.source.attr('data-jobname');
                            var targetJobName = conn.target.attr('data-jobname')
                            if(confirm('delete connection: '+ sourceJobName +" -> "+ targetJobName +'?')){
                                jQuery.ajax({
                                    url : encodeURI('edge/' + sourceJobName + '/'    + targetJobName),
                                    type : 'DELETE',
                                    success : function(response) {
                                        jsPlumb.detach(conn);
                                    },
                                    error: function (request, status, error) {
                                        alert(status+": "+error);
                                    }
                                });
                            }
                        });
                    }
                });

                jsPlumb.bind("jsPlumbConnection", function(info) {
                    jQuery.ajax({
                           url: encodeURI('edge/'+info.source.attr('data-jobname') +'/'+info.target.attr('data-jobname')),
                           type: 'PUT',
                           success: function( response ) {
//                               alert('Load was performed.');
                           },
                           error: function (request, status, error) {
                                alert(request.responseText);
                           }
                    });
                    // allow deletion of newly created connection
                    info.connection.bind("click", function(conn) {
                        var sourceJobName = conn.source.attr('data-jobname');
                        var targetJobName = conn.target.attr('data-jobname');
                        if(confirm('delete connection: '+ sourceJobName +" -> "+ targetJobName +'?')){
                            jQuery.ajax({
                                url : encodeURI('edge/' + sourceJobName + '/'    + targetJobName),
                                type : 'DELETE',
                                success : function(response) {
                                    jsPlumb.detach(conn);
                                },
                                error: function (request, status, error) {
                                    alert(request.responseText);
                                }
                            });
                        }
                    });
                });


                // make all the window divs draggable
                jsPlumb.draggable(jsPlumb.getSelector(".window"));


            });
        }
    };
})();

// start jsPlumb
jsPlumb.bind("ready", function() {
    // chrome fix.
    document.onselectstart = function () { return false; };

    jsPlumb.setRenderMode(jsPlumb.SVG);
    depview.init();
});

