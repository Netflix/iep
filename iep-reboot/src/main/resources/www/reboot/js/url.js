var nf = typeof nf !== 'undefined' ? nf : {};

nf.url = (function() {

    'use strict';

    var removeProtocol = function(url) {

        var slashesIndex = url.indexOf('//'),
            doubleSlashLength = 2;

        if (slashesIndex !== -1) {
            url = url.substr(slashesIndex + doubleSlashLength);
        }

        return url;

    };

    var getHost = function(url) {

        var host;

        if (url && url.length) {

            url = removeProtocol(url);
            host = url.split('/')[0];

        }

        return host;

    };

    var urlParamsToObj = function(url) {

        var match,
            search = /([^&=]+)=?([^&]*)/g,
            customDecode = function(s) {
                return decodeURIComponent(s.replace(/\+/g, " ")); // replacing "+" symbol with a space
            },
            query = getQuerySection(url),
            urlParams = {};

        //removing trailing slash if there is one
        if (query.substr(query.length - 1, 1) === '/') {
            query = query.substr(0, query.length - 1);
        }

        while (match = search.exec(query)) {
            urlParams[customDecode(match[1])] = customDecode(match[2]);
        }

        return urlParams;

    };

    var getHashSection = function(url) {

        var urlParts,
            hashSection = '';

        if (url && url.length) {

            urlParts = url.split('#');

            if (urlParts.length > 1) {

                // removing host, path and query section
                hashSection = urlParts[1];

            }

            //removing trailing slash if there is one
            if (hashSection.substr(hashSection.length - 1, 1) === '/') {
                hashSection = hashSection.substr(0, hashSection.length - 1);
            }

        }

        return hashSection;

    };

    var objToURLParams = function(obj) {

        var key,
            queryURLSection = '';

        for (key in obj) {

            if (obj.hasOwnProperty(key)) {

                queryURLSection += encodeURIComponent(key);
                queryURLSection += '=';
                queryURLSection += encodeURIComponent(obj[key]);
                queryURLSection += '&';

            }

        }

        //removing trailing & if there is one
        if (queryURLSection.substr(queryURLSection.length - 1, 1) === '&') {
            queryURLSection = queryURLSection.substr(0, queryURLSection.length - 1);
        }

        return queryURLSection;

    };

    var getQuerySection = function(url) {

        var urlParts,
            querySection = '';

        if (url && url.length) {

            urlParts = url.split('?');

            if (urlParts.length > 1) {

                // removing host and path
                querySection = urlParts.splice(1).join('?');

                urlParts = querySection.split('#');
                //discarding a hash fragment in case there is one after the query section
                querySection = urlParts[0];

            }

            //removing trailing slash if there is one
            if (querySection.substr(querySection.length - 1, 1) === '/') {
                querySection = querySection.substr(0, querySection.length - 1);
            }
        }

        return querySection;

    };

    var removeURLParameter = function(url, parameter) {

        var urlParts = url.split('?'),
            parameterPrefix,
            params,
            i;

        if (urlParts.length > 1) {

            parameterPrefix = encodeURIComponent(parameter) + '=';

            params = urlParts[1].split(/[&;]/g);

            //reverse iteration as may be destructive
            for (i = params.length - 1; i >= 0; i--) {

                //if params[i].startsWith(parameterPrefix)
                if (params[i].lastIndexOf(parameterPrefix, 0) === 0) {
                    params.splice(i, 1);
                }

            }

            if (params.length) {

                //assembling the URL back together without the removed parameter
                url = urlParts[0] + '?' + params.join('&');

            } else {

                url = urlParts[0];

            }

        }

        return url;

    };

    // sets (and overrides if required) a parameter into a URL string
    var setURLParameter = function(key, value, url) {

        var urlParts;

        url = removeURLParameter(url, key);

        urlParts = url.split('?');

        if (urlParts.length > 1) {

            url += '&';

        } else {

            url += '?';

        }

        url += encodeURIComponent(key) + '=' + encodeURIComponent(value);

        return url;

    };

    //url is optional
    var getParameterByName = function(name, url) {

        var searchSection = url ? getQuerySection(url) : window.location.search,
            match = RegExp('[?&]' + name + '=([^&]*)').exec(searchSection),
            param = match && decodeURIComponent(match[1].replace(/\+/g, ' '));

        //removing trailing slash if there is one
        if (param && param.substr(param.length - 1, 1) === '/') {
            param = param.substr(0, param.length - 1);
        }

        return param;

    };

    return {

        setURLParameter: setURLParameter,
        removeURLParameter: removeURLParameter,
        urlParamsToObj: urlParamsToObj,
        objToURLParams: objToURLParams,
        removeProtocol: removeProtocol,
        getQuerySection: getQuerySection,
        getHashSection: getHashSection,
        getParameterByName: getParameterByName,
        getHost: getHost

    }

}());