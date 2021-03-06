package examples.mnist;

import org.deeplearning4j.nn.graph.ComputationGraph;
import org.junit.Test;
import org.nd4j.linalg.dataset.DataSet;
import org.nd4j.linalg.factory.Nd4j;

/**
 * Test cases for {@link ResNetReferenceModel}
 *
 * @author Christian Skarby
 */
public class ResNetReferenceModelTest {

    /**
     * Test that the model can be created and that it is possible to make train for two examples
     */
    @Test
    public void fit() {
        final ComputationGraph model = new ResNetReferenceModel().create();
        model.fit(new DataSet(Nd4j.randn(1, 28*28), Nd4j.randn(1,10)));
        model.fit(new DataSet(Nd4j.randn(1, 28*28), Nd4j.randn(1,10)));
    }
}