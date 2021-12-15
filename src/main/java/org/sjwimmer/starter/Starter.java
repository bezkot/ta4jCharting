package org.sjwimmer.starter;

import java.awt.BorderLayout;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.stream.Stream;

import javax.swing.JFrame;

import org.sjwimmer.ta4jchart.chartbuilder.ChartBuilder;
import org.sjwimmer.ta4jchart.chartbuilder.ChartBuilderImpl;
import org.sjwimmer.ta4jchart.chartbuilder.ChartType;
import org.sjwimmer.ta4jchart.chartbuilder.PlotType;
import org.ta4j.core.*;
import org.ta4j.core.indicators.EMAIndicator;
import org.ta4j.core.indicators.ParabolicSarIndicator;
import org.ta4j.core.indicators.helpers.ClosePriceIndicator;
import org.ta4j.core.indicators.helpers.HighPriceIndicator;
import org.ta4j.core.indicators.helpers.VolumeIndicator;
import org.ta4j.core.rules.CrossedDownIndicatorRule;
import org.ta4j.core.rules.CrossedUpIndicatorRule;


public class Starter {

	public static void main(String[] args) {
		
		// 1 Create a barSeries, indicators and run your strategy
		BarSeries barSeries = loadAppleIncSeries();
		VolumeIndicator volume = new VolumeIndicator(barSeries);
		HighPriceIndicator highPrice = new HighPriceIndicator(barSeries);
		ParabolicSarIndicator parabolicSar = new ParabolicSarIndicator(barSeries);
		ClosePriceIndicator closePrice = new ClosePriceIndicator(barSeries);
		EMAIndicator longEma = new EMAIndicator(closePrice, 20);
		EMAIndicator shortEma = new EMAIndicator(closePrice, 6);
		CrossedDownIndicatorRule exit = new CrossedDownIndicatorRule(shortEma, longEma);
		CrossedUpIndicatorRule entry = new CrossedUpIndicatorRule(shortEma, longEma);

		Strategy strategy = new BaseStrategy(entry, exit);
		TradingRecord tradingRecord = new BarSeriesManager(barSeries).run(strategy);

		// 2 Add your ta4j objects to a ChartBuilder instance
		ChartBuilder chartBuilder = new ChartBuilderImpl(barSeries);

		chartBuilder.addIndicator(volume, PlotType.SUBPLOT, ChartType.BAR);
		chartBuilder.addIndicator(parabolicSar, PlotType.OVERLAY, ChartType.LINE);
		chartBuilder.addIndicator(longEma, PlotType.SUBPLOT, ChartType.LINE);
		chartBuilder.addIndicator(shortEma); // default: PlotType.OVERLAY, ChartType.LINE
		chartBuilder.setTradingRecord(tradingRecord);

		// 3 Create JFrame
		javax.swing.SwingUtilities.invokeLater(() -> {
		    JFrame frame = new JFrame("Ta4j charting");
		    frame.setLayout(new BorderLayout());
		    frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		    
		    // 4 add the plot to a JFrame
		    frame.add(chartBuilder.createPlot());
		    frame.pack();
		    frame.setVisible(true);
		});
	}

    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    
    /**
     * @return the bar series from Apple Inc. bars.
     */
    public static BarSeries loadAppleIncSeries() {
        return loadCsvSeries("appleinc_bars_from_20130101_usd_small.csv");
    }

    public static BarSeries loadCsvSeries(String filename) {

        URI uri;
		try {
			uri = Starter.class.getClassLoader().getResource(filename).toURI();
			BarSeries series = new BaseBarSeries("AAPL");

	        try(Stream<String> lines = Files.lines(Paths.get(uri))){
	        	lines.forEach(st -> {
		        	String[] line = st.split(",");
		            ZonedDateTime date = LocalDate.parse(line[0], DATE_FORMAT).atStartOfDay(ZoneId.systemDefault());
		            double open = Double.parseDouble(line[1]);
		            double high = Double.parseDouble(line[2]);
		            double low = Double.parseDouble(line[3]);
		            double close = Double.parseDouble(line[4]);
		            double volume = Double.parseDouble(line[5]);
		            series.addBar(date, open, high, low, close, volume);
	        	});
	        	return series;
	        }
		} catch (URISyntaxException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		return null;
    }

}
