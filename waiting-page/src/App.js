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
  const [gameVisible, setGameVisible] = useState(false)

  const identifier = new URLSearchParams(window.location.search).get('identifier');

  const showGame = (visible) => {
    console.log("show " + JSON.stringify(visible))
    if (visible.gameVisible) setGameVisible(false);
    else setGameVisible(true);
  }

  const update = () => {
    fetch(`http://localhost:8080/signature/${identifier}`)
      .then(response => response.json())
      .then(data => {
        setInfo(data.info)
        setSignature(data.signature == null ? "Waiting for signature, this can take a few minutes" : data.signature)
        

        if (data.signature != null) {
          setFrequency(null);
          setImage(done);
          setImageClass("App-logo-stop");
          setSignatureClass("signature")
        }
        else {
          setImageClass("App-logo")
          setImage(refresh)
        }
      })
      .catch((err) => {
        console.log(err)
        setInfo("We are very sorry...")
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
        <th colSpan="2">Signature</th>
      </tr>
      <tr>
        <td className='status'><img src={image} className={imageClass} alt="logo" /></td>
        <td className={signatureClass}>{signature}</td>
      </tr>
    </table>

    <div>
      <p>Want to do something while waiting?</p>
      <button onClick={() => showGame({ gameVisible })}> {gameVisible ? "Close it" : "Open up"} </button>
    </div>

    <div className='game' style={{ visibility: gameVisible ? "visible" : "hidden" }}>
      <iframe name="sudokuWindow2" src="https://123sudoku.co.uk/sudokulib/generate.php?size=large&level=3" width="500" height="640" frameBorder="0" scrolling="no">
      </iframe>
    </div>
  </div>



}

export default App;
