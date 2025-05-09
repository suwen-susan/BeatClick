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

import javax.swing.*;
import java.awt.*;
import java.util.List;

public class ChartUtils {

    public static JPanel createLeaderboardBarChart(String songId, List<ScoreRecord> records, Dimension panelSize) {
        DefaultCategoryDataset dataset = new DefaultCategoryDataset();

        for (ScoreRecord record : records) {
            dataset.addValue(record.score, "Score", record.username);  // 用户在 Y 轴，分数在 X 轴
        }

        JFreeChart chart = ChartFactory.createBarChart(
                null,               // no title
                "Score",            // X 轴标题
                "User",             // Y 轴标题
                dataset,
                PlotOrientation.HORIZONTAL,  // 横向柱状图
                false,             // legend
                false,             // tooltips
                false              // urls
        );

        CategoryPlot plot = (CategoryPlot) chart.getPlot();
        plot.setBackgroundPaint(new Color(40, 40, 50));   // 背景深色一致
        plot.setOutlineVisible(false);
        plot.setRangeGridlinePaint(Color.GRAY);           // 横线颜色

        // ✅ 调整 X 轴（Score）显示在下方
        NumberAxis xAxis = (NumberAxis) plot.getRangeAxis();
        xAxis.setLabelFont(new Font("SansSerif", Font.PLAIN, 12));
        xAxis.setLabelPaint(Color.LIGHT_GRAY);
        xAxis.setTickLabelPaint(Color.LIGHT_GRAY);
        plot.setRangeAxisLocation(AxisLocation.BOTTOM_OR_LEFT);

        // ✅ 调整 Y 轴（User）字体和颜色
        CategoryAxis yAxis = plot.getDomainAxis();
        yAxis.setLabelFont(new Font("SansSerif", Font.PLAIN, 12));
        yAxis.setTickLabelFont(new Font("SansSerif", Font.PLAIN, 12));
        yAxis.setLabelPaint(Color.LIGHT_GRAY);
        yAxis.setTickLabelPaint(Color.LIGHT_GRAY);

        // ✅ 设置条形细度
        BarRenderer renderer = (BarRenderer) plot.getRenderer();
        renderer.setMaximumBarWidth(0.1);  // 条形最大宽度比例（相对每组的空间）
        renderer.setSeriesPaint(0, new Color(100, 200, 255));  // 统一颜色
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
}


