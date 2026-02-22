import { gql } from '@apollo/client';
import { useLazyQuery } from '@apollo/client/react';
import { useState } from 'react';

type FireResult = {
  name: string;
  fireNumberFormatted: string;
  isOnTrack: boolean;
  yearsToRetirement: number;
  requiredMonthlySip: number;
  savingsRate: number;
  aiInsights: {
    executiveSummary: string;
  };
};

type FireResponse = {
  calculateFire: FireResult;
};

const CALCULATE_FIRE = gql`
  query CalculateFire($input: FireInputDTO!) {
    calculateFire(input: $input) {
      name
      fireNumberFormatted
      isOnTrack
      yearsToRetirement
      requiredMonthlySip
      savingsRate
      aiInsights {
        executiveSummary
      }
    }
  }
`;

function App() {
  const [form, setForm] = useState({
    name: '',
    currentAge: 28,
    targetRetirementAge: 50,
    annualIncome: 0,
    annualExpenses: 0,
    existingCorpus: 0,
    monthlySavings: 0,
  });

  const [calculateFire, { loading, error, data }] = useLazyQuery<FireResponse>(CALCULATE_FIRE);

  const handleChange = (e: React.ChangeEvent<HTMLInputElement>) => {
    const { name, value } = e.target;

    setForm({
      ...form,
      [name]: e.target.type === 'number' ? Number(value) : value,
    });
  };

  const handleSubmit = (e: React.FormEvent) => {
    e.preventDefault();

    calculateFire({
      variables: {
        input: form,
      },
    });
  };

  return (
    <div style={{ padding: '40px', fontFamily: 'Arial' }}>
      <h1>🔥 FIRE Calculator</h1>

      <form
        onSubmit={handleSubmit}
        style={{ marginBottom: '40px' }}
      >
        <div>
          <label>Name:</label>
          <br />
          <input
            name='name'
            onChange={handleChange}
            required
          />
        </div>

        <div>
          <label>Current Age:</label>
          <br />
          <input
            type='number'
            name='currentAge'
            onChange={handleChange}
            required
          />
        </div>

        <div>
          <label>Target Retirement Age:</label>
          <br />
          <input
            type='number'
            name='targetRetirementAge'
            onChange={handleChange}
            required
          />
        </div>

        <div>
          <label>Annual Income:</label>
          <br />
          <input
            type='number'
            name='annualIncome'
            onChange={handleChange}
            required
          />
        </div>

        <div>
          <label>Annual Expenses:</label>
          <br />
          <input
            type='number'
            name='annualExpenses'
            onChange={handleChange}
            required
          />
        </div>

        <div>
          <label>Existing Corpus:</label>
          <br />
          <input
            type='number'
            name='existingCorpus'
            onChange={handleChange}
            required
          />
        </div>

        <div>
          <label>Monthly Savings:</label>
          <br />
          <input
            type='number'
            name='monthlySavings'
            onChange={handleChange}
            required
          />
        </div>

        <br />
        <button type='submit'>Calculate FIRE</button>
      </form>

      {loading && <p>Calculating...</p>}
      {error && <p>Error: {error.message}</p>}

      {data && (
        <div style={{ border: '1px solid #ccc', padding: '20px' }}>
          <h2>📊 Result</h2>
          <p>
            <strong>Name:</strong> {data.calculateFire.name}
          </p>
          <p>
            <strong>FIRE Number:</strong> {data.calculateFire.fireNumberFormatted}
          </p>
          <p>
            <strong>On Track:</strong> {data.calculateFire.isOnTrack ? 'Yes ✅' : 'No ❌'}
          </p>
          <p>
            <strong>Years to Retirement:</strong> {data.calculateFire.yearsToRetirement}
          </p>
          <p>
            <strong>Required Monthly SIP:</strong> ₹{data.calculateFire.requiredMonthlySip}
          </p>
          <p>
            <strong>Savings Rate:</strong> {data.calculateFire.savingsRate}%
          </p>

          <h3>🤖 AI Summary</h3>
          <p>{data.calculateFire.aiInsights.executiveSummary}</p>
        </div>
      )}
    </div>
  );
}

export default App;
