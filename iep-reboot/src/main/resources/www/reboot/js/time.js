var nf = typeof nf !== 'undefined' ? nf : {};

nf.time = (function() {

    var parse = function(text, refPoint, format) {

        var epochLength = 13, //String(new Date().getTime()).length,
            number,
            unit,
            retVal,
            m;

        if (text && text.length) {
            text = text.toLowerCase().trim();
        }

        if (text.substr(0, 3) === 'now') {

            // e.g. now-3h
            number = parseInt(text.match(/\d+/), 10);
            unit = getUnitFromCharacter(text.substr(text.length -1 , 1));

            if (!isNaN(number) && unit) {

                m = moment().subtract(number, unit);

            }

        } else if (refPoint && text.substr(0, 1) === 'e') {

            // e.g. e-3h
            number = text.match(/\d+/);
            unit = text.substr(text.length -1 , 1);

            if (number instanceof Array &&
                number.length &&
                getUnitFromCharacter(unit)) {

                number = parseInt(number[0], 10);
                m = moment(refPoint).subtract(number, getUnitFromCharacter(unit));

            }

        } else if ( moment(text, format, true).isValid() ) {

            m =  moment(text, format, true);

        } else if (text.length === epochLength && !isNaN(text)) {

            // probably epoch time was passed
            m = moment(parseInt(text, 10));

        } else if (text.length !== epochLength && !isNaN(text)) {

            // any number length other than epochLength is unacceptable
            m = false;

        } else {

            m = moment(text);

        }

        if ( m && m.isValid() ){
            retVal = new Date( m.valueOf() );
        }

        return retVal;

    };

    var getUnitFromCharacter = function(letter) {

        var unit;

        switch(letter) {

            case 's':
                unit = 'seconds';
                break;
            case 'm':
                unit = 'minutes';
                break;
            case 'h':
                unit = 'hours';
                break;
            case 'd':
                unit = 'days';
                break;
            case 'w':
                unit = 'weeks';
                break;
            case 'y':
                unit = 'years';
                break;

        }

        return unit;

    };

    //rounds date to neares x milliseconds (defaults to 1 minute)
    var round = function(date, milliseconds) {

        date = date || new Date();
        milliseconds = milliseconds || (1000 * 60);

        return new Date(Math.round(date.getTime() / milliseconds) * milliseconds);

    };

    return {
        round: round,
        parse: parse,
        getUnitFromCharacter: getUnitFromCharacter
    }

}());