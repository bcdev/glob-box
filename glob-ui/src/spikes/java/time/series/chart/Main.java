package time.series.chart;

import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.renderer.xy.XYErrorRenderer;
import org.jfree.data.time.Month;
import org.jfree.data.time.TimeSeries;
import org.jfree.data.time.TimeSeriesCollection;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JPanel;
import java.awt.BasicStroke;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.awt.geom.Ellipse2D;

public class Main {

    public static void main(String[] args) {

        TimeSeries s1 = new TimeSeries("L&G European Index Trust");
        TimeSeries s2 = new TimeSeries("L&G UK Index Trust");

        TimeSeriesCollection dataset = new TimeSeriesCollection();
        dataset.addSeries(s1);
        dataset.addSeries(s2);

        s1.add(new Month(2, 2001), 181.8);
        s1.add(new Month(3, 2001), 167.3);
        s1.add(new Month(4, 2001), 153.8);
        s1.add(new Month(5, 2001), 167.6);
        s1.add(new Month(6, 2001), 158.8);
        s1.add(new Month(7, 2001), 148.3);
        s1.add(new Month(8, 2001), null);
//        s1.add(new Month(8, 2001), 153.9);
        s1.add(new Month(9, 2001), null);
//        s1.add(new Month(9, 2001), 142.7);
        s1.add(new Month(10, 2001), null);
//        s1.add(new Month(10, 2001), 123.2);
        s1.add(new Month(11, 2001), 131.8);
        s1.add(new Month(12, 2001), 139.6);
        s1.add(new Month(1, 2002), 142.9);
        s1.add(new Month(2, 2002), 138.7);
        s1.add(new Month(3, 2002), 137.3);
        s1.add(new Month(4, 2002), 143.9);
        s1.add(new Month(5, 2002), 139.8);
        s1.add(new Month(6, 2002), 137.0);
        s1.add(new Month(7, 2002), 132.8);

        s2.add(new Month(2, 2001), 129.6);
        s2.add(new Month(3, 2001), 123.2);
        s2.add(new Month(4, 2001), 117.2);
        s2.add(new Month(5, 2001), 124.1);
        s2.add(new Month(6, 2001), 122.6);
        s2.add(new Month(7, 2001), 119.2);
        s2.add(new Month(8, 2001), 116.5);
        s2.add(new Month(9, 2001), 112.7);
        s2.add(new Month(10, 2001), 101.5);
        s2.add(new Month(11, 2001), 106.1);
        s2.add(new Month(12, 2001), 110.3);
        s2.add(new Month(1, 2002), 111.7);
        s2.add(new Month(2, 2002), 111.0);
        s2.add(new Month(3, 2002), 109.6);
        s2.add(new Month(4, 2002), 113.2);
        s2.add(new Month(5, 2002), 111.6);
        s2.add(new Month(6, 2002), 108.8);
        s2.add(new Month(7, 2002), 101.6);



        final JFreeChart timeSeriesChart = ChartFactory.createTimeSeriesChart(
                    "BallaBalla", // Titel
                    "time", // time axis label
                    "chl", // value axis label
                    dataset, // XY Dataset,
                    true, // legend
                    true, // tooltips
                    false // url's
        );

        final XYPlot plot = timeSeriesChart.getXYPlot();

//        final XYLineAndShapeRenderer renderer = (XYLineAndShapeRenderer) plot.getRendererForDataset(dataset);

        final XYErrorRenderer renderer = new XYErrorRenderer();
        renderer.setDrawXError(false);
        renderer.setAutoPopulateSeriesStroke(false);
        renderer.setBaseLinesVisible(true);


        plot.setRenderer(0, renderer);
        renderer.setBaseShapesVisible(true);
        renderer.setSeriesPaint(0, Color.CYAN);
        renderer.setSeriesPaint(1, Color.GREEN);
        renderer.setSeriesShape(0, new Ellipse2D.Double(-5, -5, 10, 10));
        renderer.setBaseStroke(new BasicStroke(1.0f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND, 10.0f,
                                               new float[]{10.0f}, 0.0f));
//        renderer.setSeriesShape(0, new Ellipse2D.Double(-4,-4,8,8));


        plot.getSeriesCount();

        final Action addInsituAction = new AbstractAction("Add Insitu Time Serie") {
            @Override
            public void actionPerformed(ActionEvent e) {
                //Todo change body of created method. Use File | Settings | File Templates to change
            }
        };

        final JPanel buttonPane = new JPanel(new FlowLayout(FlowLayout.CENTER));
        buttonPane.add(new JButton(addInsituAction));

        final JPanel eastPanel = new JPanel(new BorderLayout());
        eastPanel.add(buttonPane, BorderLayout.SOUTH);

        final JPanel panel = new JPanel(new BorderLayout(5, 5));
        panel.add(new ChartPanel(timeSeriesChart));
        panel.add(eastPanel, BorderLayout.EAST);

        final JFrame frame = new JFrame("Time Series Chart");
        frame.getContentPane().add(panel);

        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setVisible(true);
        frame.pack();
    }
}
