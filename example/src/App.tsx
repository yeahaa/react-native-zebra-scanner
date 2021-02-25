import * as React from 'react';

import {
  StyleSheet,
  View,
  Text,
  TouchableOpacity,
  NativeEventEmitter,
  NativeModules,
} from 'react-native';
import ZebraScanner from 'react-native-zebra-scanner';
import { useCallback, useEffect } from 'react';

export default function App() {
  const [result, setResult] = React.useState<string | undefined>();
  const [error, setError] = React.useState<any | undefined>();

  let clear = () => {
    setResult('');
    setError('');
  };

  let setBarcode = (barcode: string) => {
    setResult(barcode);
    setError('');
  };

  let setBarcodeError = (nativeError: any) => {
    setResult('');
    setError(nativeError.message);
  };

  let testScan = (success: boolean) => {
    clear();
    ZebraScanner.testScan(success).then(setBarcode).catch(setBarcodeError);
  };

  let scanOnce = () => {
    clear();
    ZebraScanner.scanOnce().then(setBarcode).catch(setBarcodeError);
  };

  let startScanning = () => {
    clear();
    ZebraScanner.startScanning().then(setBarcode).catch(setBarcodeError);
  };

  let stopScanning = useCallback(() => {
    clear();
    ZebraScanner.stopScanning();
  }, []);

  let testScanSuccess = () => testScan(true);

  let testScanFailure = () => testScan(false);

  useEffect(() => {
    const emitter = new NativeEventEmitter(NativeModules.ZebraScanner);
    const listener = emitter.addListener('BarcodeScanned', (event) => {
      setBarcode(event.barcode);
    });

    return () => {
      stopScanning();
      listener.remove();
    };
  }, [stopScanning]);

  return (
    <View style={styles.container}>
      <Text style={styles.text}>Scan result: {result}</Text>
      <Text style={styles.text}>Error: {error}</Text>
      <TouchableOpacity
        style={[styles.button, styles.topMargin]}
        onPress={scanOnce}
      >
        <Text style={styles.buttonText}>Zebra Scan Once</Text>
      </TouchableOpacity>
      <TouchableOpacity
        style={[styles.button, styles.topMargin]}
        onPress={startScanning}
      >
        <Text style={styles.buttonText}>Zebra Start Scanning</Text>
      </TouchableOpacity>
      <TouchableOpacity style={styles.button} onPress={stopScanning}>
        <Text style={styles.buttonText}>Zebra Stop Scanning</Text>
      </TouchableOpacity>
      <TouchableOpacity
        style={[styles.button, styles.topMargin]}
        onPress={testScanSuccess}
      >
        <Text style={styles.buttonText}>TestScan Success</Text>
      </TouchableOpacity>
      <TouchableOpacity style={styles.button} onPress={testScanFailure}>
        <Text style={styles.buttonText}>TestScan Failure</Text>
      </TouchableOpacity>
      <TouchableOpacity
        style={[styles.button, styles.buttonSecondary, styles.topMargin]}
        onPress={clear}
      >
        <Text style={styles.buttonText}>Clear</Text>
      </TouchableOpacity>
    </View>
  );
}

const styles = StyleSheet.create({
  container: {
    flex: 1,
    alignItems: 'center',
    justifyContent: 'center',
  },
  text: {
    minWidth: 288,
    fontSize: 20,
  },
  button: {
    backgroundColor: '#2196f3',
    paddingVertical: 8,
    paddingHorizontal: 16,
    margin: 4,
    minWidth: 192,
    borderRadius: 8,
  },
  buttonText: {
    fontSize: 20,
    color: '#ffffff',
    textAlign: 'center',
  },
  buttonSecondary: {
    backgroundColor: '#727272',
  },
  topMargin: {
    marginTop: 24,
  },
});
