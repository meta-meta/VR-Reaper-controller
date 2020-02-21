using System.Collections;
using System.Collections.Generic;
using UnityEngine;

public class HideControllerWhenHolding : MonoBehaviour
{
    private OvrAvatarTouchController controllerLeft;
    private OvrAvatarTouchController controllerRight;
    // Start is called before the first frame update
    void Start()
    {
        var avatarTransform = GameObject.Find("LocalAvatar").transform;
        controllerLeft = avatarTransform.Find("controller_left").GetComponent<OvrAvatarTouchController>();
        controllerRight = avatarTransform.Find("controller_right").GetComponent<OvrAvatarTouchController>();
    }

    // Update is called once per frame
    void Update()
    {
        if (!controllerLeft)
        {
            var avatarTransform = GameObject.Find("LocalAvatar").transform;
            controllerLeft = avatarTransform.Find("controller_left").GetComponent<OvrAvatarTouchController>();
        }
        else
        {
            // !controllerLeft.RenderParts.TrueForAll(part => !part.enabled)
//            if (controllerLeft.RenderParts[0].GetComponent<SkinnedMeshRenderer>())
//            {
//                Debug.Log("left");
//            }
        }
        
    }
}
