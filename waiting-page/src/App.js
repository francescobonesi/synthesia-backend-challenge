import './App.css';
import { useState } from 'react';
import { useInterval } from './utils';
import refresh from './refresh.svg';
import done from './done.svg';
import error from './warning.svg';


function App() {

  const [frequency, setFrequency] = useState(10000);
  const [signature, setSignature] = useState("");
  const [info, setInfo] = useState("no info yet");
  const [image, setImage] = useState(refresh)
  const [imageClass, setImageClass] = useState("App-logo")
  const [signatureClass, setSignatureClass] = useState("signaturewait")

  const identifier = new URLSearchParams(window.location.search).get('identifier');

  const update = () => {
    fetch(`http://localhost:8080/signature/${identifier}`)
      .then(response => response.json())
      .then(data => {
        setInfo(data.info)
        setSignature(data.signature == null ? "Waiting for signature" : data.signature)
        if (data.signature != null) {
          setFrequency(null);
          setImage(done);
          setImageClass("App-logo-stop");
          setSignatureClass("signature")
        }
      })
      .catch((err) => {
        console.log(err)
        setInfo("We are very sorry...")
        setFrequency(null);
        setImage(error);
        setSignature("There is something wrong in the requested identifier")
        setImageClass("App-logo-stop");
      })
  }

  useInterval(() => {
    if (identifier == null) return;

    console.log('calling api')
    update()

  }, frequency)

  if (identifier == undefined || identifier == null) {
    setFrequency(null)
    return <div>error: missing identifier</div>
  }

  update()

  return <div className="App">

    <h2>{info}</h2>

    {/* <p>{info}</p> */}

    <table>
      <tr>
        <th colspan="2">Signature</th>
      </tr>
      <tr>
        <td className='status'><img src={image} className={imageClass} alt="logo" /></td>
        <td className={signatureClass}>{signature}</td>
      </tr>
    </table>

    {/* <iframe width="560" height="315" src="https://www.youtube.com/embed/ao-Sahfy7Hg" title="YouTube video player" frameborder="0" allow="accelerometer; autoplay; clipboard-write; encrypted-media; gyroscope; picture-in-picture" allowfullscreen></iframe> */}


  </div>



}

export default App;
