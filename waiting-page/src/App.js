import './App.css';
import { useState } from 'react';
import { useInterval } from './utils';


function App() {

  const [frequency, setFrequency] = useState(10000)
  const [signature, setSignature] = useState("no signature yet");
  const [info, setInfo] = useState("no info yet");
  const identifier = new URLSearchParams(window.location.search).get('identifier');
  
  const update = () => {
    fetch(`http://localhost:8080/signature/${identifier}`)
    .then(response => response.json())
    .then(data => {
      setInfo(data.info)
      setSignature(data.signature)
      if(data.signature != null) setFrequency(null);
    })
    .catch((err) => {
      console.log(err)
      setInfo("identifier has not been requested at all")
      setFrequency(null);
    })
  }

  useInterval(() => {
    if(identifier == null) return;
    
    console.log('calling api')
    update()

  }, frequency)

  if(identifier == undefined || identifier == null){
    setFrequency(null)
    return <div>error: missing identifier</div>
  } 

  update()

  return <div>
    <p>info: {info}</p>
    <p>signature: {signature}</p>
  </div>
  


}

export default App;
