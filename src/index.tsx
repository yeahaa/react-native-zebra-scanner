import { NativeModules } from 'react-native';

type ZebraScannerType = {
  testScan(success: boolean): Promise<string>;
  scanOnce(): Promise<string>;
  startScanning(): Promise<string>;
  stopScanning(): void;
};

const { ZebraScanner } = NativeModules;

export default ZebraScanner as ZebraScannerType;
