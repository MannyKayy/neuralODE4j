package examples.mnist;

import com.beust.jcommander.Parameter;
import org.deeplearning4j.nn.api.OptimizationAlgorithm;
import org.deeplearning4j.nn.conf.ComputationGraphConfiguration.GraphBuilder;
import org.deeplearning4j.nn.conf.ConvolutionMode;
import org.deeplearning4j.nn.conf.NeuralNetConfiguration;
import org.deeplearning4j.nn.conf.graph.ElementWiseVertex;
import org.deeplearning4j.nn.conf.inputs.InputType;
import org.deeplearning4j.nn.conf.layers.*;
import org.deeplearning4j.nn.graph.ComputationGraph;
import org.deeplearning4j.nn.weights.WeightInit;
import org.nd4j.linalg.activations.impl.ActivationIdentity;
import org.nd4j.linalg.activations.impl.ActivationReLU;
import org.nd4j.linalg.activations.impl.ActivationSoftmax;
import org.nd4j.linalg.learning.config.Nesterovs;
import org.nd4j.linalg.lossfunctions.impl.LossMCXENT;
import org.nd4j.linalg.schedule.MapSchedule;
import org.nd4j.linalg.schedule.ScheduleType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * ResNet reference model for comparison. Reimplementation of https://github.com/rtqichen/torchdiffeq/blob/master/examples/odenet_mnist.py
 *
 * @author Christian Skarby
 */
public class ResNetReferenceModel implements ModelFactory {

    private static final Logger log = LoggerFactory.getLogger(ResNetReferenceModel.class);

    @Parameter(names = "-nrofResBlocks", description = "Number of residual blocks to use")
    private int nrofResBlocks = 6;

    @Parameter(names = "-nrofKernels", description = "Number of filter kernels in each convolution layer")
    private int nrofKernels = 64;

    @Parameter(names = "-seed", description = "Random seed")
    private long seed = 666;

    private final GraphBuilder builder;

    public ResNetReferenceModel() {
        builder = new NeuralNetConfiguration.Builder()
                .seed(seed)
                .weightInit(WeightInit.RELU)
                .optimizationAlgo(OptimizationAlgorithm.STOCHASTIC_GRADIENT_DESCENT)
                .updater(new Nesterovs(
                        new MapSchedule.Builder(ScheduleType.EPOCH)
                                .add(0, 0.1)
                                .add(60, 0.01)
                                .add(100, 0.001)
                                .add(140, 0.0001)
                                .build()
                ))
                .graphBuilder()
                .setInputTypes(InputType.feedForward(28 * 28));
    }

    @Override
    public ComputationGraph create() {

        log.info("Create model");

        String next = new ConvStem(nrofKernels).add(null, builder);
        for (int i = 0; i < nrofResBlocks; i++) {
            next = addResBlock(next, i);
        }
        new Output(nrofKernels).add(next, builder);
        final ComputationGraph graph = new ComputationGraph(builder.build());
        graph.init();
        return graph;
    }


    private String addResBlock(String prev, int cnt) {
        builder
                .addLayer("normFirst_" + cnt,
                        new BatchNormalization.Builder()
                                .nOut(nrofKernels)
                                .activation(new ActivationReLU()).build(), prev)
                .addLayer("convFirst_" + cnt,
                        new Convolution2D.Builder(3, 3)
                                .nOut(nrofKernels)
                                .activation(new ActivationIdentity())
                                .convolutionMode(ConvolutionMode.Same)
                                .build(), "normFirst_" + cnt)
                .addLayer("normSecond_" + cnt,
                        new BatchNormalization.Builder()
                                .nOut(nrofKernels)
                                .activation(new ActivationIdentity()).build(), "convFirst_" + cnt)
                .addLayer("convSecond_" + cnt,
                        new Convolution2D.Builder(3, 3)
                                .nOut(nrofKernels)
                                .activation(new ActivationIdentity())
                                .convolutionMode(ConvolutionMode.Same)
                                .build(), "normSecond_" + cnt)
                .addVertex("add_" + cnt, new ElementWiseVertex(ElementWiseVertex.Op.Add), prev, "convSecond_" + cnt);
        return "add_" + cnt;
    }

    private void addOutput(String prev) {
        builder
                .addLayer("normOutput",
                        new BatchNormalization.Builder()
                                .nOut(nrofKernels)
                                .activation(new ActivationReLU()).build(), prev)
                .addLayer("globPool", new GlobalPoolingLayer.Builder().poolingType(PoolingType.AVG).build(), "normOutput")
                .addLayer("output", new OutputLayer.Builder()
                        .nOut(10)
                        .lossFunction(new LossMCXENT())
                        .activation(new ActivationSoftmax()).build(), "globPool")
                .setOutputs("output");
    }

}
