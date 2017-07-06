$(function() {
  $(".button-checkbox").each(function() {
    const $widget = $(this),
      $button = $widget.find("button"),
      $checkbox = $widget.find("input:checkbox"),
      color = $button.data("color"),
      settings = {
        on: {
          icon: "glyphicon glyphicon-check"
        },
        off: {
          icon: "glyphicon glyphicon-unchecked"
        }
      };
    $("#submit").on("click", function() {
      console.log("pressed");
      const isChecked = $checkbox.is(":checked");
      if (isChecked) {
        Cookies.set("rememberUsername", $("#Username").val());
        Cookies.set("rememberPassword", $("#Password").val());
      } else {
        Cookies.remove("rememberUsername");
        Cookies.remove("rememberPassword");
      }
    });
    $button.on("click", function() {
      $checkbox.prop("checked", !$checkbox.is(":checked"));
      Cookies.set("loginRememberButton", $checkbox.is(":checked"));
      $checkbox.triggerHandler("change");
      updateDisplay();
    });

    $checkbox.on("change", function() {
      updateDisplay();
    });

    function updateDisplay() {
      const isChecked = $checkbox.is(":checked");
      // Set the button's state
      $button.data("state", isChecked ? "on" : "off");

      // Set the button's icon
      $button
        .find(".state-icon")
        .removeClass()
        .addClass("state-icon " + settings[$button.data("state")].icon);

      // Update the button's colo
      if (isChecked) {
        $button.removeClass("btn-default").addClass("btn-" + color + " active");
      } else {
        $button.removeClass("btn-" + color + " active").addClass("btn-default");
      }
    }

    function showLoginData() {
      const isChecked = $checkbox.is(":checked");
      if (isChecked) {
        $("#Username").val(Cookies.get("rememberUsername") || "");
        $("#Password").val(Cookies.get("rememberPassword") || "");
      }
    }

    function init() {
      $checkbox.prop(
        "checked",
        JSON.parse(Cookies.get("loginRememberButton") || "false")
      );
      showLoginData();
      updateDisplay();
      // Inject the icon if applicable
      if ($button.find(".state-icon").length === 0) {
        $button.prepend(
          '<i class="state-icon ' +
            settings[$button.data("state")].icon +
            '"></i> '
        );
      }
    }

    init();
  });
});
