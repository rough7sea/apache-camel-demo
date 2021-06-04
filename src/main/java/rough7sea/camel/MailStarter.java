package rough7sea.camel;

import org.apache.camel.CamelContext;
import org.apache.camel.ProducerTemplate;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.impl.DefaultCamelContext;

public class MailStarter {
    public static void main(String[] args) throws Exception {

        CamelContext camel = new DefaultCamelContext();
        camel.getPropertiesComponent().setLocation("classpath:application.properties");

        mailRoute(camel);

        camel.start();

        ProducerTemplate template = camel.createProducerTemplate();
        template.sendBodyAndHeaders("direct:mail", "Hello from camel!", null);

        camel.stop();

    }

    public static void mailRoute(CamelContext ctx) throws Exception {
        ctx.addRoutes(new RouteBuilder() {
            @Override
            public void configure() {
                from("direct:mail")
                        .setHeader("subject", simple("Something about Camel"))
                        .setHeader("to", simple("danila.karnatsevich@unidata-platform.org"))
                        .to("smtps://{{mail.host}}:{{mail.port}}?username={{mail.userName}}&password={{mail.password}}");
            }
        });
    }
}
