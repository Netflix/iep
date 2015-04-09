var nf = typeof nf !== 'undefined' ? nf : {};

nf.asyncNode = (function() {

    'use strict';

    var retryDelay = 300;

    var getNode = function(nodeHint, fallbackSelector) {

        return new Promise(function(resolve, reject) {

            var keepQueryingForNode = function() {

                var node = queryNode(nodeHint, fallbackSelector);

                if (node) {

                    resolve(node);

                } else if (node === false) {

                    reject( Error('Not enough information was provided in order to find the relevant node') );

                } else {

                    // relevant node has not yet been found, retry in a little bit (probably template has not run yet)
                    window.setTimeout(function() {

                        keepQueryingForNode();

                    }, retryDelay);

                }

            };

            keepQueryingForNode()

        });

    };

    var queryNode = function(nodeHint, fallbackSelector) {

        var selector,
            targetNode = false;

        if (nodeHint) {

            if (typeof nodeHint === "string") {

                selector = nodeHint.indexOf('#') === 0 ? nodeHint : "#" + nodeHint;
                targetNode = document.querySelector(selector);

            } else if (typeof nodeHint === "object") {

                targetNode = nodeHint;

            }

        } else if (fallbackSelector) {

            targetNode = document.querySelector(fallbackSelector);

        }

        return targetNode;

    };

    return {

        get: getNode

    }

}());