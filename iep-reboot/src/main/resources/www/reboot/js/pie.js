var nf = typeof nf !== 'undefined' ? nf : {};

nf.pie = function(config) {

    config = config || {};

    var set = {};

    var disabled = {
        enabled: false
    };

    var enabled = {
        enabled: true
    };

    var defaultChartConfig = {

        chart: {
            plotBackgroundColor: null,
            plotBorderWidth: null,
            plotShadow: false
        },
        tooltip: {
            shared: false
        },

        credits      : disabled,
        legend       : enabled,

        plotOptions: {
            pie: {
                allowPointSelect: true,
                cursor: 'pointer',
                dataLabels: {
                    enabled: true,
                    style: {
                        color: (Highcharts.theme && Highcharts.theme.contrastTextColor) || 'black'
                    },
                    format: '<b>{point.name}</b>: {point.percentage:.1f} %'
                }
            }
        },

        series: [{
            type: 'pie',
            data: []
        }]

    };

    var processConfig = function() {

        var customConfig = {

            chart : {
                renderTo: config.container,
                animation: typeof config.animate === 'undefined' ? false : config.animate
            },
            title: {
                text: config.title || null
            }

        };

        return customConfig;

    };

    this.chartConfig = nf.utils.extend(defaultChartConfig, processConfig() );

    this.addSlice = function(name, value, animate) {

        animate = typeof animate === 'undefined' ? true : animate;

        if (set[name]) {

            this.chart.get(name).update({
                name: name,
                y: value
            }, true , animate);

        } else {

            set[name] = [name, value];

            this.chart.series[0].addPoint({
                name: name,
                y: value,
                id: name
            });

        }

    };

    this.chart = new Highcharts.Chart(this.chartConfig);

};