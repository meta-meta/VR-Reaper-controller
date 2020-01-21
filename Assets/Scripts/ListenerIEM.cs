using System;
using System.Collections;
using System.Collections.Generic;
using System.Linq;
using UnityEngine;

//[ExecuteInEditMode]
public class ListenerIEM : MonoBehaviour
{
    public int PortIn;
    public int PortOut;
    public int PortOutSceneRotator;
    
    private GameObject _nametag;

    // The GameObject with an AudioObjectIEM we will treat as the "master" for room controls
//    public GameObject RoomMaster;

    private OscIn _oscInRoomEncoder;
    private OscOut _oscOutRoomEncoder;
    private OscOut _oscOutSceneRotator;

    private Vector3 _rot;
    private Vector3 _pos;

    private OscMessage _yaw;
    private OscMessage _pitch;
    private OscMessage _roll;
    
    private OscMessage _listenerX;
    private OscMessage _listenerY;
    private OscMessage _listenerZ;
    
    private void Start()
    {
        if (PortOut > 0)
        {
//            _oscOutRoomEncoder = RoomMaster.GetComponent<OscOut>();

            _oscOutRoomEncoder = gameObject.AddComponent<OscOut>();
            _oscOutRoomEncoder.Open(PortOut, "192.168.1.17"); // TODO: move somewhere else
            
            _listenerX = new OscMessage($"/RoomEncoder/listenerX");
            _listenerY = new OscMessage($"/RoomEncoder/listenerY");
            _listenerZ = new OscMessage($"/RoomEncoder/listenerZ");
        }
        
        if (PortOutSceneRotator > 0)
        {
            _oscOutSceneRotator = gameObject.AddComponent<OscOut>();
            _oscOutSceneRotator.Open(PortOutSceneRotator, "192.168.1.17"); // TODO: move somewhere else
            
            _yaw = new OscMessage($"/SceneRotator/yaw");
            _pitch = new OscMessage($"/SceneRotator/pitch");
            _roll = new OscMessage($"/SceneRotator/roll");
        }

        if (PortIn > 0)
        {
            _oscInRoomEncoder = gameObject.AddComponent<OscIn>();
            _oscInRoomEncoder.Open(PortIn);
            
            _oscInRoomEncoder.MapFloat($"/RoomEncoder/listenerX", OnReceiveListenerX );
            _oscInRoomEncoder.MapFloat($"/RoomEncoder/listenerY", OnReceiveListenerY );
            _oscInRoomEncoder.MapFloat($"/RoomEncoder/listenerZ", OnReceiveListenerZ );
        }
    }

    void OnReceiveListenerX(float val)
    {
        transform.position = new Vector3(val,transform.position.y,transform.position.z);
    }
    
    void OnReceiveListenerY(float val) // IEM Y = Unity Z
    {
        transform.position = new Vector3(transform.position.x,transform.position.y,val);
    }
    
    void OnReceiveListenerZ(float val) // IEM Z = Unity Y
    {
        transform.position = new Vector3(transform.position.x,val,transform.position.z);
    }

    private string _name;
    private void UpdateNametag()
    {
        if (_name != gameObject.name)
        {
            if (!_nametag) _nametag = transform.Find("Nametag").gameObject;
            _name = gameObject.name;
            _nametag.GetComponent<TextMesh>().text = _name;
        }
    }

    static float Scale(float inMin, float inMax, float outMin, float outMax, float val) =>
        Mathf.Lerp(
            outMin,
            outMax,
            Mathf.InverseLerp(inMin, inMax, val));

    private void SendPositionToRoomEncoder()
    {
        _pos = transform.position;
        _listenerX.Set(0, _pos.x);
        _listenerY.Set(0, _pos.z); // IEM Y = Unity Z
        _listenerZ.Set(0, _pos.y); // IEM Z = Unity Y
        _oscOutRoomEncoder.Send(_listenerX);
        _oscOutRoomEncoder.Send(_listenerY);
        _oscOutRoomEncoder.Send(_listenerZ);

        _rot = transform.rotation.eulerAngles;
        
        _yaw.Set(0, Scale(0, 360, -180, 180, _rot.y));
        _pitch.Set(0, Scale(0, 360, -180, 180, _rot.x));
        _roll.Set(0, Scale(0, 360, -180, 180, _rot.z));
        _oscOutSceneRotator.Send(_yaw);
        _oscOutSceneRotator.Send(_pitch);
        _oscOutSceneRotator.Send(_roll);
    }
    
    void Update()
    {
        SendPositionToRoomEncoder();
    }
}