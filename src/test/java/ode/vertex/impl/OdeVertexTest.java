package ode.vertex.impl;

import org.deeplearning4j.nn.conf.ConvolutionMode;
import org.deeplearning4j.nn.conf.NeuralNetConfiguration;
import org.deeplearning4j.nn.conf.inputs.InputType;
import org.deeplearning4j.nn.conf.layers.*;
import org.deeplearning4j.nn.graph.ComputationGraph;
import org.junit.Test;
import org.nd4j.linalg.activations.impl.ActivationIdentity;
import org.nd4j.linalg.activations.impl.ActivationReLU;
import org.nd4j.linalg.api.ndarray.INDArray;
import org.nd4j.linalg.dataset.DataSet;
import org.nd4j.linalg.factory.Nd4j;
import org.nd4j.linalg.learning.config.Sgd;

import static org.junit.Assert.assertNotEquals;

/**
 * Test cases for {@link OdeVertex} (and config of the same)
 */
public class OdeVertexTest {

    /**
     * Smoke test to see that it is possible to do a forward pass
     */
    @Test
    public void doForward() {
        final long nOut = 8;
        final ComputationGraph graph = new ComputationGraph(new NeuralNetConfiguration.Builder()
                .graphBuilder()
                .addInputs("input")
                .setInputTypes(InputType.convolutional(9, 9, 1))
                .addLayer("0",
                        new Convolution2D.Builder(3, 3)
                                .nOut(nOut)
                                .convolutionMode(ConvolutionMode.Same).build(), "input")
                .addVertex("1", new ode.vertex.conf.OdeVertex.Builder("ode0",
                        new BatchNormalization.Builder().build())
                        .addLayer("ode1",
                                new Convolution2D.Builder(3, 3)
                                        .nOut(nOut)
                                        .convolutionMode(ConvolutionMode.Same).build(), "ode0")
                        .build(), "0")
                .addLayer("2", new BatchNormalization.Builder().build(), "1")
                .setOutputs("output")
                .addLayer("output", new CnnLossLayer(), "2")
                .build());

        graph.init();
        INDArray output = graph.outputSingle(Nd4j.randn(new long[]{1, 1, 9, 9}));
        assertNotEquals("Expected non-zero output!", 0, output.sumNumber());
    }

    /**
     * Smoke test to see that it is possible to fit
     */
    @Test
    public void fit() {
        final long nOut = 8;
        final ComputationGraph graph = new ComputationGraph(new NeuralNetConfiguration.Builder()
                .updater(new Sgd(0.1))
                .graphBuilder()
                .addInputs("input")
                .setInputTypes(InputType.convolutional(9, 9, 1))
                .addLayer("0",
                        new Convolution2D.Builder(3, 3)
                                .nOut(nOut)
                                .activation(new ActivationIdentity())
                                .convolutionMode(ConvolutionMode.Same).build(), "input")
                .addLayer("bn",   new BatchNormalization.Builder()
                        .activation(new ActivationReLU())
                        .build(), "0")
                .addVertex("1", new ode.vertex.conf.OdeVertex.Builder("ode0",
                        new BatchNormalization.Builder()
                                .activation(new ActivationReLU())
                                .build())
                        .addLayer("ode1",
                                new Convolution2D.Builder(3, 3)
                                        .nOut(nOut)
                                        .activation(new ActivationIdentity())
                                        .convolutionMode(ConvolutionMode.Same).build(), "ode0")
                        .build(), "bn")
                .addLayer("2", new BatchNormalization.Builder()
                        .activation(new ActivationReLU())
                        .build(), "1")
                .addLayer("gp", new GlobalPoolingLayer.Builder().build(), "2")
                .setOutputs("output")
                .addLayer("output", new OutputLayer.Builder().nOut(3).build(), "gp")
                .build());

        graph.init();
        final INDArray before = graph.getVertex("1").params().dup();
        graph.fit(new DataSet(Nd4j.randn(new long[]{1, 1, 9, 9}), Nd4j.create(new double[] {0,1,0})));
        assertNotEquals("Expected parameters to be updated!", before, graph.getVertex("1").params().dup());
    }
}