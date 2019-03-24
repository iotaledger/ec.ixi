const $inputs = {};
let $tables = {};

// namespaces
const Gui = {};
const Gen = {};
const Api = {};
const Btn = {};

window.onload = function () {
    init_elements();
    init_functions();
    Gui.refresh_wallet();
    Gui.refresh_actors();
    Gui.refresh_cluster();
    Gui.refresh_transfers();
};

function init_elements() {

    $('input').each((_, $el) => { $inputs[$el.id] = $($el); });
    val("seed", random_trytes(81));
    val("merkle_tree_depth",3);
    val("cluster_trust", 0.2);
}

function get_table(id) {
    if(!$tables[id])
        $tables[id] = $('#'+id+" table");
    return $tables[id];
}

function init_functions() {

    const shorten = (hash) => $("<div>").text(hash.substr(0, 30) + "…")
        .append(Gen.gen_cell_button("copy", () => {copy_to_clipboard(hash)}));

    const prepare_transfer = (seed, index, address, balance) => {
        val("transfers_seed", seed);
        val("transfers_index", index);
        val("transfers_sender", address);
        val("transfers_receiver", "");
        val("transfers_remainder", "");
        val("transfers_value", balance);
        Gui.show("new_transfer")
    };

    const wallet_serialize = (entry, index) => [
        "#"+index,
        shorten(entry['address']),
        entry['balance'],
        $("<div>")
            .append(Gen.gen_cell_button("send", () => {prepare_transfer(val("seed"), index, entry['address'], entry['balance'])}))
    ];

    const actors_serialize = actor => [
        shorten(actor),
        Gen.gen_cell_button("✘", () => { Api.remove_actor(actor) })
    ];

    const cluster_serialize = entry => [
        shorten(entry['address']),
        entry['trust'],
        $("<div>").append(Gen.gen_cell_button("markers", () => { Gui.refresh_markers(entry['address']); Gui.show("markers"); }))
            .append(Gen.gen_cell_button("✘", () => {Api.set_trust(entry['address'], 0)}))
    ];

    const transfers_serialize = hash => [
        shorten(hash),
        $("<div>").append(Gen.gen_cell_button("status", () => { Gui.load_transactions(hash) }))
            .append(Gen.gen_cell_button("✘", () => { Api.remove_transfer(hash) }))
    ];

    const transactions_serialize = entry => [
        shorten(entry['hash']),
        shorten(entry['address']),
        entry['value'],
        $("<div>").text(entry['confidence']).append(Gen.gen_cell_button("details", () => { Gui.display_confidences(entry['confidences']); Gui.show("confidences"); }))
    ];

    const confidences_serialize = entry => [
        shorten(entry['actor']),
        entry['confidence']
    ];

    const markers_serialize = entry => [
        shorten(entry['ref1']),
        shorten(entry['ref2']),
        entry['confidence']
    ];

    Gui.display_balances = Gen.gen_display_function('wallet', ["index", "address", "balance", ""], wallet_serialize);
    Gui.display_actors = Gen.gen_display_function("actors", ["address", ""], actors_serialize);
    Gui.display_cluster = Gen.gen_display_function("cluster", ["address", "trust", ""], cluster_serialize);
    Gui.display_transfers = Gen.gen_display_function("transfers", ["transfer", ""], transfers_serialize);
    Gui.display_transactions = Gen.gen_display_function("transactions", ["hash", "address", "value", "confidence"], transactions_serialize);
    Gui.display_confidences = Gen.gen_display_function("confidences", ["actor", "confidence"], confidences_serialize);
    Gui.display_markers = Gen.gen_display_function("markers", ["reference #1", "reference #2", "confidence"], markers_serialize);
}

/* ***** BUTTON ACTIONS ***** */

Btn.submit_transfer = () => {
    if(!Gui.validate_form('new_transfer'))
        return;
    const seed = val("transfers_seed");
    const index = parseInt(val("transfers_index"));
    const receiver = val("transfers_receiver");
    const remainder = val("transfers_remainder");
    const value = val("transfers_value");
    Api.submit_transfer(seed, index, receiver, remainder, value, () => { Gui.refresh_transfers(); Gui.hide("new_transfer") });
};

Btn.create_actor = () => {
    if(!Gui.validate_form('actors'))
        return;
    const merkle_tree_depth = parseInt(val("merkle_tree_depth"));
    const seed = random_trytes(81);
    Api.create_actor(seed, merkle_tree_depth, 0);
};

Btn.set_trust = () => {
    if(!Gui.validate_form('cluster'))
        return;
    const address = val("cluster_address");
    const trust = parseFloat(val("cluster_trust"));
    Api.set_trust(address, trust);
};

/* ***** HELPERS ***** */

function val(input, val = null) {
    return val === null ? $inputs[input].val() : $inputs[input].val(val);
}

function random_trytes(length) {
    let seed = "";
    for(let i = 0; i < length; i++)
        seed += random_tryte();
    return seed;
}

function random_tryte() {
    const TRYTES = "ABCDEFGHIJKLMNOPQRSTUVWXYZ9";
    return TRYTES.charAt(Math.floor(Math.random()*27));
}

function copy_to_clipboard(message) {
    let input = document.createElement('input');
    input.value = message;
    document.body.appendChild(input);
    input.select();
    document.execCommand('copy');
    document.body.removeChild(input);
}

/* ***** GUI ***** */

Gui.refresh_markers = (actor) => Api.get_markers(actor, Gui.display_markers);
Gui.refresh_wallet = () => Api.get_balances(val("seed"), Gui.display_balances);
Gui.refresh_actors = () => Api.get_actors(Gui.display_actors);
Gui.refresh_cluster = () => Api.get_cluster(Gui.display_cluster);
Gui.refresh_transfers = () => Api.get_transfers(Gui.display_transfers);

Gui.load_transactions = (bundle_head) => {
    Api.get_transactions(bundle_head, (transactions) => { Gui.display_transactions(transactions); Gui.show("transactions"); });
};

Gui.handle_error = function (message) {
    Swal.fire({title: "Whoops!", html: message, type: "error"});
    console.log(message);
};

Gui.hide = (id) => { $('#'+id).addClass("hidden"); };
Gui.show = (id) => { $('#'+id).removeClass("hidden"); };

Gui.validate_form = (id) => {
    const $form_inputs = $('#'+id+" input");
    for(let i = 0; i < $form_inputs.length; i++) {
        const $el = $($form_inputs[i]);
        const pattern = $el.attr("pattern");
        const regex = new RegExp(pattern);
        if(pattern && !$el.val().match(regex)) {
            $el.addClass("invalid");
            Gui.handle_error("field <code>" + $el.attr("placeholder") + "</code> does not match expected pattern: <code>" + pattern+"</code>");
            return false;
        }
        $el.removeClass("invalid");
    }

    return true;
};

/* ***** GENERATORS ***** */

Gen.gen_display_function = function (table_name, table_head, object_serializer) {
    let $table = get_table(table_name);
    return function(objects) {
        $table.html(Gen.gen_table_row(table_head, true));
        $.each(objects, function (index, object) {
            $table.append(Gen.gen_table_row(object_serializer(object, index)));
        });
    };
};

/**
 * @param cells array of row's cell contents in correct order
 * @param th if enabled will use <th> cells instead of <td>
 * @return table row whose cells are filled with respective content
 * */
Gen.gen_table_row = function (cells, th = false) {
    const $tr = $("<tr>");
    $.each(cells, function (_, cell) {
        $tr.append($(th ? "<th>" : "<td>").html(cell));
    });
    return $tr;
};

Gen.gen_cell_button = (name, onclick) => $("<input>").attr("type", "button").val(name).click(onclick);

/* ***** API WRAPPERS ***** */

Api.submit_transfer = function (seed, index, receiver, remainder, value, callback) {
    const request ={
        "action": "submit_transfer",
        "seed": seed,
        "index": index,
        "receiver": receiver,
        "remainder": remainder,
        "value": value,
        "check_balances": false
    };
    Api.ec_request(request, response => callback(response['hash']));
};

Api.get_transfers = function (callback) {
    Api.ec_request({"action": "get_transfers"}, response => callback(response['transfers']));
};

Api.get_transactions = function (bundle_head, callback) {
    Api.ec_request({"action": "get_transactions", "bundle_head": bundle_head}, response => callback(response['transactions']));
};

Api.get_balances = function (seed, callback) {
    Api.ec_request({"action": "get_balances", "seed": seed}, response => callback(response['balances']));
};

Api.get_actors = function (callback) {
    Api.ec_request({"action": "get_actors"}, response => callback(response['actors']));
};

Api.get_cluster = function (callback) {
    Api.ec_request({"action": "get_cluster"}, response => callback(response['cluster']));
};

Api.get_markers = function (actor, callback) {
    Api.ec_request({"action": "get_markers", "actor": actor}, response => callback(response['markers']));
};

Api.create_actor = function (seed, merkle_tree_depth, start_index) {
    Api.ec_request({"action": "create_actor", "seed": seed, "merkle_tree_depth": merkle_tree_depth, "start_index": start_index}, Gui.refresh_actors);
};

Api.remove_actor = function (address) {
    Api.ec_request({"action": "delete_actor", "address": address}, Gui.refresh_actors);
};

Api.remove_transfer = function (transfer) {
    Api.ec_request({"action": "unwatch_transfer", "transfer": transfer}, Gui.refresh_transfers);
};

Api.set_trust = function (address, trust) {
    Api.ec_request({"action": "set_trust", "address": address, "trust": trust}, Gui.refresh_cluster);
};

/* ***** API ***** */

Api.ec_request = (request, success) => {
    Api.ajax("getModuleResponse", {"request": JSON.stringify(request), "path": "ec.ixi-1.0.jar"}, data => {
        const response = JSON.parse(data['response']);
        response['success'] ? (success ? success(response) : {}) : Gui.handle_error("api error: " + response['error'].replace(/[A-Z9]{81}/g, "<code class='hash'>$&</code>"));
    });
};

Api.ajax = (path, data, success) => {
    data['password'] = "change_me_now";
    $.ajax({
        url: "http://localhost:2187/" + path,
        method: "POST",
        data: Api.serialize_post_data(data),
        dataType: "json",
        success: success,
        error: (error) => {
            Gui.handle_error(JSON.stringify(error));
        }
    });
};

Api.serialize_post_data = (data) => {
    const serialized = [];
    $.each(data, function (name, value) {
        serialized.push({"name": name, "value": value});
    });
    return serialized;
};