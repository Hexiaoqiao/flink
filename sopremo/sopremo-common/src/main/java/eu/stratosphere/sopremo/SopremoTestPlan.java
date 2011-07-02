package eu.stratosphere.sopremo;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

import org.codehaus.jackson.JsonNode;

import eu.stratosphere.pact.common.contract.DataSinkContract;
import eu.stratosphere.pact.common.contract.DataSourceContract;
import eu.stratosphere.pact.common.plan.PactModule;
import eu.stratosphere.pact.common.type.Key;
import eu.stratosphere.pact.common.type.KeyValuePair;
import eu.stratosphere.pact.common.type.Value;
import eu.stratosphere.pact.testing.TestPairs;
import eu.stratosphere.pact.testing.TestPlan;
import eu.stratosphere.sopremo.base.PersistenceType;
import eu.stratosphere.sopremo.base.Sink;
import eu.stratosphere.sopremo.base.Source;
import eu.stratosphere.sopremo.pact.JsonInputFormat;
import eu.stratosphere.sopremo.pact.PactJsonObject;
import eu.stratosphere.util.ConversionIterator;
import eu.stratosphere.util.dag.OneTimeTraverser;

public class SopremoTestPlan {
	public static class MockupSource extends Source {

		/**
		 * 
		 */
		private static final long serialVersionUID = -7149952920902388869L;

		private int index;

		public MockupSource(int index) {
			super(PersistenceType.HDFS, "mockup-input" + index);
			this.index = index;
		}

		@Override
		public PactModule asPactModule(EvaluationContext context) {
			PactModule pactModule = new PactModule(this.toString(), 0, 1);
			DataSourceContract contract = TestPlan.createDefaultSource(this.getInputName());
			pactModule.getOutput(0).setInput(contract);
			// pactModule.setInput(0, contract);
			return pactModule;
		}

		@Override
		public int hashCode() {
			final int prime = 31;
			int result = super.hashCode();
			result = prime * result + this.index;
			return result;
		}

		@Override
		public boolean equals(Object obj) {
			if (this == obj)
				return true;
			if (!super.equals(obj))
				return false;
			if (this.getClass() != obj.getClass())
				return false;
			MockupSource other = (MockupSource) obj;
			return this.index == other.index;
		}

		@Override
		public String toString() {
			return String.format("MockupSource [%s]", this.index);
		}
	}

	public static class MockupSink extends Sink {
		/**
		 * 
		 */
		private static final long serialVersionUID = -8095218927711236381L;

		private int index;

		public MockupSink(int index) {
			super(PersistenceType.ADHOC, "mockup-output" + index, null);
			this.index = index;
		}

		@Override
		public PactModule asPactModule(EvaluationContext context) {
			PactModule pactModule = new PactModule(this.toString(), 1, 0);
			DataSinkContract contract = TestPlan.createDefaultSink(this.getOutputName());
			contract.setInput(pactModule.getInput(0));
			pactModule.addInternalOutput(contract);
			return pactModule;
		}

		@Override
		public int hashCode() {
			final int prime = 31;
			int result = super.hashCode();
			result = prime * result + this.index;
			return result;
		}

		@Override
		public boolean equals(Object obj) {
			if (this == obj)
				return true;
			if (!super.equals(obj))
				return false;
			if (this.getClass() != obj.getClass())
				return false;
			MockupSink other = (MockupSink) obj;
			return this.index == other.index;
		}

		@Override
		public String toString() {
			return String.format("MockupSink [%s]", this.index);
		}
	}

	//
	// public static interface TestObjects extends Iterable<PactJsonObject> {
	// public TestObjects add(PactJsonObject object);
	//
	// public TestObjects setEmpty();
	// }

	static class Channel<O extends Operator, C extends Channel<O, C>> implements Iterable<PactJsonObject> {
		private TestPairs<PactJsonObject.Key, PactJsonObject> pairs = new TestPairs<PactJsonObject.Key, PactJsonObject>();

		private O operator;

		private int index;

		public Channel(O operator, int index) {
			this.operator = operator;
			this.index = index;
		}

		O getOperator() {
			return this.operator;
		}

		void setOperator(O operator) {
			if (operator == null)
				throw new NullPointerException("operator must not be null");

			this.operator = operator;
			setEmpty();
		}

		void setPairs(Iterable<PactJsonObject> pairs) {
			this.pairs = new TestPairs<PactJsonObject.Key, PactJsonObject>();
			for (PactJsonObject pactJsonObject : pairs)
				this.pairs.add(PactJsonObject.Key.NULL, pactJsonObject);
		}

		@SuppressWarnings("unchecked")
		public C add(PactJsonObject object) {
			this.pairs.add(PactJsonObject.Key.NULL, object);
			return (C) this;
		}

		@Override
		public Iterator<PactJsonObject> iterator() {
			return new ConversionIterator<KeyValuePair<PactJsonObject.Key, PactJsonObject>, PactJsonObject>(
				this.pairs.iterator()) {
				@Override
				protected PactJsonObject convert(KeyValuePair<PactJsonObject.Key, PactJsonObject> inputObject) {
					return inputObject.getValue();
				}
			};
		}

		int getIndex() {
			return this.index;
		}

		@SuppressWarnings("unchecked")
		public C setEmpty() {
			this.pairs.setEmpty();
			return (C) this;
		}

		TestPairs<PactJsonObject.Key, PactJsonObject> getPairs() {
			return this.pairs;
		}

		@Override
		public int hashCode() {
			final int prime = 31;
			int result = 1;
			result = prime * result + (this.pairs == null ? 0 : this.pairs.hashCode());
			return result;
		}

		@Override
		public boolean equals(Object obj) {
			if (this == obj)
				return true;
			if (obj == null)
				return false;
			if (!(obj instanceof Channel))
				return false;
			Channel<?, ?> other = (Channel<?, ?>) obj;
			return this.pairs.equals(other.pairs);
		}

		@Override
		public String toString() {
			return pairs.toString();
		}
	}

	public static class Input extends Channel<Source, Input> {
		public Input(int index) {
			super(new MockupSource(index), index);
		}

		public void prepare(TestPlan testPlan) {
			if (this.getOperator() instanceof MockupSource)
				testPlan.getInput(this.getIndex()).add(this.getPairs());
		}
	}

	public static class ExpectedOutput extends Channel<Source, ExpectedOutput> {
		public ExpectedOutput(int index) {
			super(new MockupSource(index), index);
		}

		public void prepare(TestPlan testPlan) {
			if (this.getOperator() instanceof MockupSource)
				testPlan.getExpectedOutput(this.getIndex()).add(this.getPairs());
		}
	}

	public static class ActualOutput extends Channel<Sink, ActualOutput> {
		public ActualOutput(int index) {
			super(new MockupSink(index), index);
		}

		public void load(TestPlan testPlan) {
			setEmpty();
			TestPairs<Key, Value> actualOutput = testPlan.getActualOutput(getIndex());
			for (KeyValuePair<Key, Value> keyValuePair : actualOutput)
				add((PactJsonObject) keyValuePair.getValue());
			actualOutput.close();
		}
	}

	private Input[] inputs;

	private ActualOutput[] actualOutputs;

	private ExpectedOutput[] expectedOutputs;

	public SopremoTestPlan(int numInputs, int numOutputs) {
		initInputsAndOutputs(numInputs, numOutputs);
	}

	protected void initInputsAndOutputs(int numInputs, int numOutputs) {
		this.inputs = new Input[numInputs];
		for (int index = 0; index < numInputs; index++)
			this.inputs[index] = new Input(index);
		this.expectedOutputs = new ExpectedOutput[numOutputs];
		for (int index = 0; index < numOutputs; index++)
			this.expectedOutputs[index] = new ExpectedOutput(index);
		this.actualOutputs = new ActualOutput[numOutputs];
		for (int index = 0; index < numOutputs; index++)
			this.actualOutputs[index] = new ActualOutput(index);
	}

	public SopremoTestPlan(Operator... sinks) {
		List<Operator.Output> unconnectedOutputs = new ArrayList<Operator.Output>();
		List<Operator> unconnectedInputs = new ArrayList<Operator>();
		for (Operator operator : sinks)
			unconnectedOutputs.addAll(operator.getOutputs());

		for (Operator operator : OneTimeTraverser.INSTANCE.getReachableNodes(sinks, OperatorNavigator.INSTANCE)) {
			if (operator instanceof Source)
				unconnectedInputs.add(operator);
			else
				for (Operator.Output input : operator.getInputs())
					if (input == null)
						unconnectedInputs.add(operator);
		}

		this.inputs = new Input[unconnectedInputs.size()];
		for (int index = 0; index < this.inputs.length; index++) {
			this.inputs[index] = new Input(index);
			Operator unconnectedNode = unconnectedInputs.get(index);
			if (unconnectedNode instanceof Source)
				setInputOperator(index, (Source) unconnectedNode);
			// this.inputs[index].setOperator((Source) unconnectedNode);
			else {
				List<Operator.Output> missingInputs = new ArrayList<Operator.Output>(unconnectedNode.getInputs());
				for (int missingIndex = 0; missingIndex < sinks.length; missingIndex++)
					if (missingInputs.get(missingIndex) == null)
						missingInputs.set(missingIndex, this.inputs[index++].getOperator().getOutput(0));
				unconnectedNode.setInputs(missingInputs);
			}
		}
		this.actualOutputs = new ActualOutput[unconnectedOutputs.size()];
		this.expectedOutputs = new ExpectedOutput[unconnectedOutputs.size()];
		for (int index = 0; index < this.actualOutputs.length; index++) {
			this.actualOutputs[index] = new ActualOutput(index);
			if (unconnectedOutputs.get(index).getOperator() instanceof Sink)
				this.actualOutputs[index].setOperator((Sink) unconnectedOutputs.get(index).getOperator());
			else
				this.actualOutputs[index].getOperator().setInput(0, unconnectedOutputs.get(index));
			this.expectedOutputs[index] = new ExpectedOutput(index);
		}
	}

	public Input getInput(int index) {
		return this.inputs[index];
	}

	public Source getInputOperator(int index) {
		return this.getInput(index).getOperator();
	}

	public Source[] getInputOperators(int from, int to) {
		Source[] operators = new Source[to - from];
		for (int index = 0; index < operators.length; index++)
			operators[index] = this.getInputOperator(from + index);
		return operators;
	}

	public Sink[] getOutputOperators(int from, int to) {
		Sink[] operators = new Sink[to - from];
		for (int index = 0; index < operators.length; index++)
			operators[index] = this.getOutputOperator(from + index);
		return operators;
	}

	public ExpectedOutput getExpectedOutput(int index) {
		return this.expectedOutputs[index];
	}

	public ActualOutput getActualOutput(int index) {
		return this.actualOutputs[index];
	}

	public Sink getOutputOperator(int index) {
		return this.getActualOutput(index).getOperator();
	}

	public void setOutputOperator(int index, Sink operator) {
		this.actualOutputs[index].setOperator(operator);

		TestPairs<PactJsonObject.Key, PactJsonObject> testPairs = new TestPairs<PactJsonObject.Key, PactJsonObject>();
		testPairs.fromFile(JsonInputFormat.class, operator.getOutputName());
		for (KeyValuePair<PactJsonObject.Key, PactJsonObject> kvPair : testPairs)
			this.inputs[index].add(kvPair.getValue());
		testPairs.close();
	}

	public void setInputOperator(int index, Source operator) {
		this.inputs[index].setOperator(operator);
		if (operator.getType() == PersistenceType.ADHOC)
			for (JsonNode node : operator.getAdhocValues())
				this.inputs[index].add(new PactJsonObject(node));
		else {
			TestPairs<PactJsonObject.Key, PactJsonObject> testPairs = new TestPairs<PactJsonObject.Key, PactJsonObject>();
			testPairs.fromFile(JsonInputFormat.class, operator.getInputName());
			for (KeyValuePair<PactJsonObject.Key, PactJsonObject> kvPair : testPairs)
				this.inputs[index].add(kvPair.getValue());
			testPairs.close();
		}
	}

	private transient TestPlan testPlan;

	public void run() {
		SopremoPlan sopremoPlan = new SopremoPlan(this.getOutputOperators(0, this.expectedOutputs.length));
		this.testPlan = new TestPlan(sopremoPlan.assemblePact());
		for (Input input : this.inputs)
			input.prepare(this.testPlan);
		for (ExpectedOutput output : this.expectedOutputs)
			output.prepare(this.testPlan);
		this.testPlan.run();
		for (ActualOutput output : this.actualOutputs)
			output.load(this.testPlan);
	}

	@Override
	public String toString() {
		return SopremoModule.valueOf("", this.getOutputOperators(0, this.actualOutputs.length)).toString();
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + Arrays.hashCode(inputs);
		result = prime * result + Arrays.hashCode(actualOutputs);
		result = prime * result + Arrays.hashCode(expectedOutputs);
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		SopremoTestPlan other = (SopremoTestPlan) obj;
		return Arrays.equals(inputs, other.inputs) && Arrays.equals(expectedOutputs, other.expectedOutputs)
			&& Arrays.equals(actualOutputs, other.actualOutputs);
	}

}
