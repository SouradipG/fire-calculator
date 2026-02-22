import { gql } from '@apollo/client';
import { useQuery } from '@apollo/client/react';

type FireResponse = {
  calculateFire: {
    fireNumber: number;
    isOnTrack: boolean;
  };
};

const CALCULATE_FIRE = gql`
  query {
    calculateFire(input: { name: "Somraj", currentAge: 28, targetRetirementAge: 50, annualIncome: 1200000, annualExpenses: 600000, existingCorpus: 2000000, monthlySavings: 50000 }) {
      fireNumber
      isOnTrack
    }
  }
`;

function App() {
  const { loading, error, data } = useQuery<FireResponse>(CALCULATE_FIRE);

  if (loading) return <p>Loading...</p>;
  if (error) return <p>Error: {error.message}</p>;
  if (!data) return <p>No data available</p>;

  return (
    <div>
      <h1>FIRE Result</h1>
      <p>Fire Number: {data.calculateFire.fireNumber}</p>
      <p>On Track: {data.calculateFire.isOnTrack ? 'Yes' : 'No'}</p>
    </div>
  );
}

export default App;
