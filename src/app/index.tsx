import { Text, View } from 'react-native';

import { useCall } from '@/hooks/use-call';

export default function Home() {
  const inCall = useCall();
  console.log(inCall);

  return (
    <View style={{ flex: 1, alignItems: 'center', justifyContent: 'center' }}>
      <Text>{inCall}</Text>
    </View>
  );
}
