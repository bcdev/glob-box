package time.series.chart;

import com.bc.jexp.ParseException;
import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.event.ChartChangeEvent;
import org.jfree.chart.event.ChartChangeEventType;
import org.jfree.chart.event.ChartChangeListener;
import org.jfree.chart.event.TitleChangeEvent;
import org.jfree.chart.event.TitleChangeListener;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.renderer.xy.XYErrorRenderer;
import org.jfree.chart.title.Title;
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

public class TimeSeriesGraphMain {

    public static void main(String[] args) throws ParseException {

        final TimeSeries s1 = new TimeSeries("L&G European Index Trust");

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

        final TimeSeries insituEmpty = new TimeSeries("insitu empty");

        final TimeSeries s2 = new TimeSeries("L&G UK Index Trust");

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

        final TimeSeries s3 = new TimeSeries("L&G UK Index Trust_blah");

        s3.add(new Month(2, 2001), 29.6);
        s3.add(new Month(3, 2001), 23.2);
        s3.add(new Month(4, 2001), 17.2);
        s3.add(new Month(5, 2001), 24.1);
        s3.add(new Month(6, 2001), 22.6);
        s3.add(new Month(7, 2001), 0);
        s3.add(new Month(8, 2001), 0);
        s3.add(new Month(9, 2001), 12.7);
        s3.add(new Month(10, 2001), 0);
        s3.add(new Month(11, 2001), 6.1);
        s3.add(new Month(12, 2001), 10.3);
        s3.add(new Month(1, 2002), 11.7);
        s3.add(new Month(2, 2002), 11.0);

        final TimeSeriesCollection dataset1 = new TimeSeriesCollection();
        dataset1.addSeries(s1);
        dataset1.addSeries(insituEmpty);

        final TimeSeriesCollection dataset2 = new TimeSeriesCollection();
        dataset2.addSeries(s2);
        dataset2.addSeries(s3);


        final JFreeChart timeSeriesChart = ChartFactory.createTimeSeriesChart(
                    "BallaBalla", // Titel
                    "time", // time axis label
                    "chl", // value axis label
                    dataset1, // XY Dataset,
                    true, // legend
                    true, // tooltips
                    false // url's
        );

        timeSeriesChart.getLegend().addChangeListener(new TitleChangeListener() {
            @Override
            public void titleChanged(TitleChangeEvent event) {
                final Title title = event.getTitle();
            }
        });

        timeSeriesChart.addChangeListener(new ChartChangeListener() {
            @Override
            public void chartChanged(ChartChangeEvent event) {
                //Todo change body of created method. Use File | Settings | File Templates to change
                final ChartChangeEventType chartChangeEventType = event.getType();
            }
        });

        final XYPlot plot = timeSeriesChart.getXYPlot();
        plot.setDataset(0, dataset1);
        plot.setDataset(1, dataset2);

//        final XYLineAndShapeRenderer renderer = (XYLineAndShapeRenderer) plot.getRendererForDataset(dataset);

        final XYErrorRenderer renderer1 = createRenderer();
        plot.setRenderer(0, renderer1);
        final XYErrorRenderer renderer2 = createRenderer();
        plot.setRenderer(1, renderer2);


        renderer1.setDrawXError(false);
        renderer1.setDrawYError(false);
        renderer1.setSeriesPaint(0, Color.BLACK);
        renderer1.setBaseLinesVisible(false);
        renderer1.setBaseShapesFilled(false);
        renderer1.setSeriesVisibleInLegend(1, false);

        renderer2.setDrawXError(false);
        renderer2.setSeriesLinesVisible(0, true);
        renderer2.setSeriesShapesVisible(0, true);
        renderer2.setSeriesStroke(0, new BasicStroke());
        renderer2.setSeriesPaint(0, Color.GREEN);
        renderer2.setSeriesLinesVisible(1, true);
        renderer2.setSeriesShapesVisible(1, true);
        renderer2.setSeriesStroke(1, new BasicStroke());
        renderer2.setSeriesPaint(1, Color.ORANGE);

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

    private static XYErrorRenderer createRenderer() {
        final XYErrorRenderer renderer = new XYErrorRenderer();
        renderer.setAutoPopulateSeriesFillPaint(false);
        renderer.setAutoPopulateSeriesOutlinePaint(false);
        renderer.setAutoPopulateSeriesOutlineStroke(false);
        renderer.setAutoPopulateSeriesPaint(false);
        renderer.setAutoPopulateSeriesShape(false);
        renderer.setAutoPopulateSeriesStroke(false);
        return renderer;
    }
}
