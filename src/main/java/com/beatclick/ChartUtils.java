package com.beatclick;

import org.jfree.chart.*;
import org.jfree.chart.plot.CategoryPlot;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.renderer.category.BarRenderer;
import org.jfree.chart.axis.CategoryAxis;
import org.jfree.chart.axis.NumberAxis;
import org.jfree.chart.axis.AxisLocation;
import org.jfree.chart.renderer.category.StandardBarPainter;
import org.jfree.data.category.DefaultCategoryDataset;
import org.jfree.data.general.DefaultPieDataset;
import org.jfree.data.category.DefaultCategoryDataset;
import javax.swing.*;
import java.awt.*;
import java.util.List;

import org.jfree.chart.plot.PiePlot;
import org.jfree.chart.labels.StandardPieSectionLabelGenerator;

public class ChartUtils {

    public static JPanel createLeaderboardBarChart(String songId, List<ScoreRecord> records, Dimension panelSize) {
        DefaultCategoryDataset dataset = new DefaultCategoryDataset();

        final int MAX_RECORDS = 10;
        List<ScoreRecord> limited = records.size() > MAX_RECORDS
                ? records.subList(0, MAX_RECORDS)
                : records;

        for (ScoreRecord record : limited) {
            dataset.addValue(record.score, "Score", record.username);
        }

        JFreeChart chart = ChartFactory.createBarChart(
                "Top 10 Player",               // no title
                "Score",            // X
                "Player Name",             // Y
                dataset,
                PlotOrientation.HORIZONTAL,  // horizontal
                false,             // legend
                false,             // tooltips
                false              // urls
        );

        CategoryPlot plot = (CategoryPlot) chart.getPlot();
        plot.setBackgroundPaint(new Color(40, 40, 50));
        plot.setOutlineVisible(false);
        plot.setRangeGridlinePaint(Color.GRAY);

        NumberAxis xAxis = (NumberAxis) plot.getRangeAxis();
        xAxis.setLabelFont(new Font("SansSerif", Font.PLAIN, 12));
        xAxis.setLabelPaint(Color.DARK_GRAY);
        xAxis.setTickLabelPaint(Color.DARK_GRAY);
        plot.setRangeAxisLocation(AxisLocation.BOTTOM_OR_LEFT);

        CategoryAxis yAxis = plot.getDomainAxis();
        yAxis.setLabelFont(new Font("SansSerif", Font.PLAIN, 12));
        yAxis.setTickLabelFont(new Font("SansSerif", Font.PLAIN, 12));
        yAxis.setLabelPaint(Color.DARK_GRAY);
        yAxis.setTickLabelPaint(Color.DARK_GRAY);

        BarRenderer renderer = (BarRenderer) plot.getRenderer();
        renderer.setMaximumBarWidth(0.1);
        renderer.setSeriesPaint(0, new Color(100, 200, 255));
        ChartPanel chartPanel = new ChartPanel(chart, false);
        renderer.setBarPainter(new StandardBarPainter());
        renderer.setShadowVisible(false);
        chartPanel.setPreferredSize(new Dimension(
                Math.min(panelSize.width - 40, 500),
                Math.min(panelSize.height - 40, 300)
        ));
        chartPanel.setBackground(new Color(30, 30, 40));

        return chartPanel;
    }

    public static JPanel createRatingPieChart(int excellent, int good, int poor, int miss) {
        DefaultPieDataset dataset = new DefaultPieDataset();
        dataset.setValue("Excellent", excellent);
        dataset.setValue("Good", good);
        dataset.setValue("Poor", poor);
        dataset.setValue("Miss", miss);

        JFreeChart chart = ChartFactory.createPieChart(
                "Rating Distribution",
                dataset,
                true, true, false
        );


        PiePlot plot = (PiePlot) chart.getPlot();
    
        // Set the label font and color
        plot.setSectionPaint("Excellent", new Color(255, 215, 0));  // Gold Excellent
        plot.setSectionPaint("Good", new Color(46, 204, 113));      // Green Good
        plot.setSectionPaint("Poor", new Color(52, 152, 219));      // Blue Poor
        plot.setSectionPaint("Miss", new Color(231, 76, 60));       // Red Miss
        
        
        ChartPanel panel = new ChartPanel(chart);
        panel.setPreferredSize(new Dimension(500, 300));

        return panel;
    }



    public static JPanel createRatingComparisonBarChart(
            int excellent, int good, int poor, int miss,
            ScoreRecord best,
            String currentLabel, String bestLabel, String chartTitle,
            Dimension panelSize){

        DefaultCategoryDataset dataset = new DefaultCategoryDataset();

        // current score player's rating
        dataset.addValue(excellent, currentLabel, "Excellent");
        dataset.addValue(good, currentLabel, "Good");
        dataset.addValue(poor, currentLabel, "Poor");
        dataset.addValue(miss, currentLabel, "Miss");

        // history best player's rating
        dataset.addValue(best.excellentCount, bestLabel, "Excellent");
        dataset.addValue(best.goodCount, bestLabel, "Good");
        dataset.addValue(best.poorCount, bestLabel, "Poor");
        dataset.addValue(best.missCount, bestLabel, "Miss");

        JFreeChart chart = ChartFactory.createBarChart(
                chartTitle,
                "Rating",
                "Count",
                dataset,
                PlotOrientation.VERTICAL,
                true, false, false
        );

        CategoryPlot plot = chart.getCategoryPlot();
        plot.setBackgroundPaint(new Color(40, 40, 50));
        plot.setOutlineVisible(false);
        plot.setRangeGridlinePaint(Color.GRAY);

        NumberAxis yAxis = (NumberAxis) plot.getRangeAxis();
        yAxis.setLabelFont(new Font("SansSerif", Font.PLAIN, 12));
        yAxis.setLabelPaint(Color.DARK_GRAY);
        yAxis.setTickLabelPaint(Color.DARK_GRAY);

        CategoryAxis xAxis = plot.getDomainAxis();
        xAxis.setLabelFont(new Font("SansSerif", Font.PLAIN, 12));
        xAxis.setTickLabelFont(new Font("SansSerif", Font.PLAIN, 12));
        xAxis.setLabelPaint(Color.DARK_GRAY);
        xAxis.setTickLabelPaint(Color.DARK_GRAY);

        BarRenderer renderer = (BarRenderer) plot.getRenderer();
        renderer.setMaximumBarWidth(0.15);
        renderer.setSeriesPaint(0, new Color(100, 200, 255));  // You
        renderer.setSeriesPaint(1, new Color(180, 100, 255));  // High scorer
        renderer.setBarPainter(new StandardBarPainter());
        renderer.setShadowVisible(false);

        ChartPanel chartPanel = new ChartPanel(chart, false);
        chartPanel.setPreferredSize(panelSize);
        chartPanel.setBackground(new Color(30, 30, 40));

        return chartPanel;
    }



}


