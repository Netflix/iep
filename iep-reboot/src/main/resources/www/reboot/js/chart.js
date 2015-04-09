var nf = typeof nf !== 'undefined' ? nf : {};

nf.chart = function(config) {

    config = config || {};

    Highcharts.setOptions({
        global: {
            useUTC: false
        }
    });

    var self = this;

    this.selectedSeries = {};
    this.seriesMap = new Map();

    var disabled = {
        enabled: false
    };

    var enabled = {
        enabled: true
    };

    this.destroy = function() {

        this.chart.destroy();

    };

    var highligherEvent = {

        selection: function(event) {

            var customEvent,
                xMin,
                xMax;

            if (event.xAxis) {

                //temp solution until code is refactored to use require.js
                if (nf.time) {

                    //rounding to nearest minute
                    xMin = nf.time.round(new Date(event.xAxis[0].min)).getTime();
                    xMax = nf.time.round(new Date(event.xAxis[0].max)).getTime();

                } else {

                    xMin = event.xAxis[0].min;
                    xMax = event.xAxis[0].max;

                }

                //remove old band
                this.xAxis[0].removePlotBand('plot-band');

                //update plot band with selected region
                this.xAxis[0].addPlotBand({
                    from : xMin,
                    to   : xMax,
                    color: 'rgba(255,255,255,0.1)',
                    borderColor: '#fff',
                    borderWidth: '1px',
                    id   : 'plot-band'
                });

                customEvent = new CustomEvent('selection', {'detail': {'start': xMin, 'end': xMax}});

                this.container.dispatchEvent(customEvent);

                return false;

            }

        }

    };

    var defaultChartConfig = {

        global: {
            useUTC: false
        },

        tooltip: {
            shared: false
        },

        title: {},

        xAxis: {
            type: 'datetime'
        },

        credits      : disabled,
        rangeSelector: disabled,
        scrollbar    : disabled,

        plotOptions: {

            series: {
                step: true,
                animation: false,
                lineWidth: 1,
                states: {
                    hover: {
                        lineWidth: 2
                    },
                    select: {
                        lineWidth: 2
                    }
                },
                events: {}
            }

        }

    };

    var processConfig = function() {

        var customConfig = {

            chart: {
                renderTo : config.container,
                zoomType : 'x',
                animation: false,
                height   : typeof config.height !== 'undefined' ? parseInt(config.height, 10) : undefined,
                width    : typeof config.width !== 'undefined' ? parseInt(config.width, 10) : undefined
            },

            navigator: config.navigator ? enabled : disabled,
            legend   : config.legend ? enabled : disabled

        };


        if (config.highlighter) {
            customConfig.chart.events = highligherEvent
        }

        return customConfig;

    };

    this.chartConfig = nf.utils.extend( defaultChartConfig, processConfig() );


    /////////
    if (config.stack) {
        this.chartConfig.plotOptions.series.stacking = 'normal';
    }

    this.chartConfig.plotOptions.series.events.legendItemClick = function() {

        self.selectedSeries[this.name] = !this.visible;

    };

    if (this.chartConfig.legend.enabled) {

        this.chartConfig.legend.maxHeight = 70;

        this.chartConfig.legend.navigation = {
            activeColor: '#fff',
            animation: false,
            arrowSize: 12,
            inactiveColor: '#fff'

        };

    }

    if (config.yLog) {

        this.chartConfig.yAxis = {

            type: 'logarithmic'

        };

    }

    ////////////

    this.addSeries = function(data) {

        this.selectedSeries[data.label] = true;

        this.chart.addSeries({

            name: data.label,
            id: data.label,
            data: data.data.values,
            pointStart: data.start,
            pointInterval: data.step

        });

    };

    // call this instead of addSeries if data is being streamed in (e.g. there is a chance of data being added to an
    // existing series
    this.streamSeries = function(data) {

        var series,
            id = data.label;

        // append data to existing series if already has been added (useful for SSE events)
        if (this.seriesMap.get(id)) {

            series = this.chart.get(id);

            series.update({

                name: data.label,
                id: id,
                data: this.seriesMap.get(id).data.values.concat(data.data.values),
                pointInterval: data.step

            }, true);

        } else {

            this.addSeries(data, id);
            this.seriesMap.set(id, data);

        }

    };

    // this should be called if streaming data into the chart, once streaming has finished in order to release memory
    this.doneStreaming = function() {
        this.seriesMap = null;
    };

    this.loadSeriesStatus = function(selectedSeries) {

        var series,
            currentSeries;

        for (series in selectedSeries) {

            currentSeries = this.chart.get(series);

            if (selectedSeries[series]) {

                currentSeries.show();
                this.selectedSeries[series] = true;


            } else {

                currentSeries.hide();
                this.selectedSeries[series] = false;

            }

        }

    };

    this.load = function(payload) {

        var self = this;

        payload.forEach(function(data) {

            self.addSeries(data)

        });

    };

    this.setSelection = function(from , to) {

        //remove old band
        this.chart.xAxis[0].removePlotBand('plot-band');

        //update plot band with selected region
        this.chart.xAxis[0].addPlotBand({
            from : from,
            to   : to,
            color: 'rgba(255,255,255,0.1)',
            borderColor: '#fff',
            borderWidth: '1px',
            id   : 'plot-band'
        });

    };

    this.chart = new Highcharts.StockChart(this.chartConfig);

};