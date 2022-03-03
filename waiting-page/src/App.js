import './App.css';
import { useState } from 'react';
import { useInterval } from './utils';
import refresh from './refresh.svg';
import done from './done.svg';
import error from './warning.svg';


function App() {

  const [frequency, setFrequency] = useState(10000);
  const [signature, setSignature] = useState("no signature yet");
  const [info, setInfo] = useState("no info yet");
  const [image, setImage] = useState(refresh)
  const [imageClass, setImageClass] = useState("App-logo")
  const identifier = new URLSearchParams(window.location.search).get('identifier');
  const title = "Signature waiting website"

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
        }
      })
      .catch((err) => {
        console.log(err)
        setInfo("Sorry, there is something wrong in the requested identifier.")
        setFrequency(null);
        setImage(error);
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

    <h2>{title}</h2>

    <p>{info}</p>

    <table>
      <tr>
        <th>Signature</th>
        <th></th>
      </tr>
      <tr>
        <td>{signature}</td>
        <td><img src={image} className={imageClass} alt="logo" /></td>
      </tr>
    </table>

    {/* <iframe width="560" height="315" src="https://www.youtube.com/embed/ao-Sahfy7Hg" title="YouTube video player" frameborder="0" allow="accelerometer; autoplay; clipboard-write; encrypted-media; gyroscope; picture-in-picture" allowfullscreen></iframe> */}


  </div>



}

export default App;
