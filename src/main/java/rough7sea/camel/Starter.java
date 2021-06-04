package rough7sea.camel;

import java.io.FileInputStream;
import java.io.InputStream;

import org.apache.camel.CamelContext;
import org.apache.camel.Message;
import org.apache.camel.ProducerTemplate;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.impl.DefaultCamelContext;
import org.apache.camel.support.DefaultMessage;
import org.springframework.jdbc.datasource.DriverManagerDataSource;

public class Starter {
    public static void main(String[] args) throws Exception {
        CamelContext camel = new DefaultCamelContext();

        camel.getPropertiesComponent().setLocation("classpath:application.properties");

//        addJDBCCase(camel);
//        addFileCase(camel);
        orderCase(camel);

        camel.start();

        ProducerTemplate template = camel.createProducerTemplate();
        InputStream orderInputStream = new FileInputStream(
                "src/main/resources/files/order.xml");

        template.sendBody("direct:DistributeOrderDSL", orderInputStream);

//        sendTemplate(camel);
//        Thread.sleep(4_000);

        camel.stop();
    }

    private static void orderCase(CamelContext camel) throws Exception {

        camel.addRoutes(new RouteBuilder() {
            @Override
            public void configure() throws Exception {
                from("direct:DistributeOrderDSL")
                        .split(xpath("//order[@product='soaps']/items"))
//                        .to("stream:out");
                        .to("file:src/main/resources/order?fileName=soaps-${date:now:yyyy_MM_dd}.txt");
            }
        });
    }

    private static void sendTemplate(CamelContext camel){

        ProducerTemplate template = camel.createProducerTemplate();

        template.sendBody(
                "file:src/main/resources/files?fileName=event-${date:now:yyyy_MM_dd-HH_mm}.html",
                "<hello>Hello camel and again!</hello>"
        );

    }

    private static void addFileCase(CamelContext camel) throws Exception {
        camel.addRoutes(new RouteBuilder() {
            @Override
            public void configure() throws Exception {
                from("file:{{from}}")
                        .routeId("File processing")
//                        .log(">>>> ${body}")
                        .convertBodyTo(String.class)
                        .to("log:?showBody=true&showHeaders=true")
                        .choice()
                        .when(exchange -> ((String) exchange.getIn().getBody()).contains("=a"))
                        .to("file:{{toA}}")
                        .when(exchange -> ((String) exchange.getIn().getBody()).contains("=b"))
                        .to("file:{{toB}}");
            }
        });
    }

    private static void addJDBCCase(CamelContext camel) throws Exception {

        DriverManagerDataSource dataSource = new DriverManagerDataSource(
                "jdbc:postgresql://localhost:5432/sarafan?user=postgres&password=root"
        );

        camel.getRegistry().bind("sarafan", dataSource);


        camel.addRoutes(new RouteBuilder() {
            @Override
            public void configure() {
                from("timer:base?period=60000")
                        .routeId("JDBC route")
                        .setHeader("key", constant(6))
                        .setBody(simple("select id, text from message where id > :?key"))
                        .to("jdbc:sarafan?useHeadersAsParameters=true")
                        .log(">>> ${body}")
                        .process(exchange -> {
                            Message in = exchange.getIn();
                            Object body = in.getBody();

                            DefaultMessage message = new DefaultMessage(exchange);

                            message.setHeaders(in.getHeaders());
                            message.setHeader("rnd", "kek");
                            message.setBody(body.toString() + "\n" + in.getHeaders().toString());

                            exchange.setMessage(message);
                        })
                        .toD("file:/src/main/resources/files/toB?fileName=done-${date:now:yyyyMMdd}-${headers.rnd}.txt");
            }
        });
    }
}
