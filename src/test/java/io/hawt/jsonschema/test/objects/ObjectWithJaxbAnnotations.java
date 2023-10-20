package io.hawt.jsonschema.test.objects;

import java.util.ArrayList;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement(name = "foo")
public class ObjectWithJaxbAnnotations {

    @XmlElement(name = "SomeProp", required = true)
    private String someProp;

    @XmlElement(name = "SomeOtherProp", defaultValue = "SomeDefault Value", type = java.lang.String.class)
    private String someOtherProp;

    @XmlElement(name = "AThirdProp")
    private String foobar;

    @XmlElement(name = "ListOStuff", required = true)
    private ArrayList<String> listOStuff;

}
